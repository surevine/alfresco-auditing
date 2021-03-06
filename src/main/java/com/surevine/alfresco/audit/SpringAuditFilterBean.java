/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.surevine.alfresco.audit;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.SessionUser;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.web.app.servlet.AuthenticationHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.StopWatch;

import com.surevine.alfresco.audit.listeners.AuditEventListener;
import com.surevine.alfresco.audit.listeners.GetAuditEventListener;
import com.surevine.alfresco.audit.listeners.ImmediateArchiveAuditEventListener;
import com.surevine.alfresco.audit.listeners.MultiDocumentDeleteAuditEventListener;
import com.surevine.alfresco.audit.listeners.SafeMoveDocumentAuditEventListener;
import com.surevine.alfresco.audit.listeners.UndeleteAuditEventListener;
import com.surevine.alfresco.audit.listeners.UnlockDocumentAuditEventListener;

/**
 * Main entry point for the filter based auditing. This class will be executed by a Spring proxy installed as a filter -
 * an instance of this class is injected into the proxy.
 *
 * It will identify at most one auditable event for any single call to the doFilter method based on the set of listeners
 * that are installed, again via spring config.
 *
 * @author garethferrier
 *
 */
public class SpringAuditFilterBean implements Filter {

    /**
     * Logger for errors and warnings.
     */
    private static final Log logger = LogFactory.getLog(SpringAuditFilterBean.class);

    @SuppressWarnings("serial")
    private static final Set<String> MULTI_READ_HTTP_METHODS = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) {
        {
            // Enable MultiRead for PUT and POST requests
            add("PUT");
            add("POST");
        }
    };

    private Set<AuditEventListener> listeners;

    /**
     * Spring available setter.
     *
     * @param listeners
     *            the set of AuditEventListeners to be configured with.
     */
    public void setListeners(final Set<AuditEventListener> listeners) {
        this.listeners = listeners;
    }

    private AuditRepository repository;

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public void setAuthenticationService(AuthenticationService authService) {
        this.authenticationService = authService;
    }

    private AuthenticationService authenticationService;

    /**
     * Spring available setter.
     *
     * @param repository
     */
    public void setRepository(final AuditRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        // Unused lifecycle method.
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = null;
        BufferedHttpServletResponse httpServletResponse = null;

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            httpServletRequest = (HttpServletRequest) request;
        } else {
            throw new ServletException(new IllegalArgumentException("Invalid request or response parameter provided."));
        }

        String method = httpServletRequest.getMethod();

        // Only override current HttpServletRequest with custom implementation if post data
        // will be read.
        if (MULTI_READ_HTTP_METHODS.contains(method)) {
            httpServletRequest = new MultiReadHttpServletRequest(httpServletRequest);
        }

        // Now iterate over each of the installed listeners searching to see if, firstly the http methods match
        // and secondly that an event is fired.
        for (AuditEventListener listener : listeners) {

            if (listener.getMethod().equals(method) && listener.isEventFired(httpServletRequest)) {

                // Need to allow the output to be read twice from the response
                httpServletResponse = new BufferedHttpServletResponse((HttpServletResponse) response);

                List<Auditable> itemsToAudit = null;
                String username = null;
                try {

                    HttpSession sess = httpServletRequest.getSession();
                    SessionUser user = (SessionUser) sess.getAttribute(AuthenticationHelper.AUTHENTICATION_USER);
                    if (user != null) {
                        username = user.getUserName();
                    }

                    // Used to track total processing time for the request being audited
                    StopWatch timer = new StopWatch();

                    // There are certain listener types where we have to construct the audit items prior to passing to
                    // the filter
                    // chain.
                    if (listener instanceof GetAuditEventListener
                            || listener instanceof MultiDocumentDeleteAuditEventListener
                            || listener instanceof SafeMoveDocumentAuditEventListener
                            || listener instanceof UnlockDocumentAuditEventListener
                            || listener instanceof UndeleteAuditEventListener
                            || listener instanceof ImmediateArchiveAuditEventListener) {
                        itemsToAudit = listener
                                .populateAuditItems(httpServletRequest, httpServletResponse);

                        // Continue with the filter chain
                        timer.start();
                        filterChain.doFilter(httpServletRequest, httpServletResponse);
                        timer.stop();

                        // Calling finish on the response will release the output stream back to the client.
                        httpServletResponse.finish();

                    } else {
                        // Populate the audit item after the filter chain has been run
                        timer.start();
                        filterChain.doFilter(httpServletRequest, httpServletResponse);
                        timer.stop();

                        // Calling finish on the response will release the output stream back to the client.
                        httpServletResponse.finish();

                        itemsToAudit = listener
                                .populateAuditItems(httpServletRequest, httpServletResponse);

                    }

                    for (Auditable audit : itemsToAudit) {
                        listener.decideSuccess(httpServletResponse, audit);
                        audit.setUser(username);
                        audit.setTimeSpent(timer.getTotalTimeMillis());
                        repository.audit(audit);
                    }

                } catch (JSONException e) {
                    logger.error("JSONException caught during audit, " + e.getMessage());
                    throw new ServletException(e);
                } catch (AlfrescoRuntimeException alfrescoRuntime) {
                    logger.error("AlfrescoRuntimeException caught during audit, " + alfrescoRuntime.getMessage());
                    throw new ServletException(alfrescoRuntime);
                } catch (DataIntegrityViolationException e) {
                    logger.error("Data Integrity Exception caught during audit, " + e.getMessage());
                    throw new ServletException("A Data Integreity Violation occured.  Please see the logs for more information");

                } catch (Exception e) {

                    logger.error("Exception caught during audit " + e.getMessage());
                    throw new ServletException(e);
                }

                return;
            }
        }

        // If we fall out here there was no auditable event so simply complete the filter chain.
        // And we won't have tinkered with the response object so just add the servletresponse
        // as the parameter.
        filterChain.doFilter(httpServletRequest, response);
    }

    /**
     * {@inheritDoc}
     */
    public void init(final FilterConfig arg0) throws ServletException {
        // Unused lifecycle method.
    }
}
