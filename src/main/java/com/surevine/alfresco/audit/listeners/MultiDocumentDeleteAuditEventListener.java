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
package com.surevine.alfresco.audit.listeners;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.surevine.alfresco.audit.AlfrescoJSONKeys;
import com.surevine.alfresco.audit.AuditItem;
import com.surevine.alfresco.audit.Auditable;
import com.surevine.alfresco.audit.BufferedHttpServletResponse;

/**
 * Class to listen for auditable events caused by the multiple deletion of documents.
 * 
 * This class extends the PostAuditEventListener which looks wrong, but is right. When doing multi deletes Alfresco
 * cannot reference the node explicitly in the URI and use the explicit DELETE method. Instead the node references are
 * encoded into the POST data.
 * 
 * @author garethferrier
 * 
 */
public class MultiDocumentDeleteAuditEventListener extends PostAuditEventListener {

    /**
     * Unique part of the URI used to resolve the signature.
     */
    public static final String URI_DESIGNATOR = "doclib/action/files";

    /**
     * Re-use the same action as the document deleted listener.
     */
    public static final String ACTION = "DOCUMENT_DELETED";

    /**
     * Default constructor.
     */
    public MultiDocumentDeleteAuditEventListener() {
        super(URI_DESIGNATOR, ACTION, METHOD);
    }

    @Override
    public void setSpecificAuditMetadata(final Auditable auditable, final HttpServletRequest request,
            final JSONObject json, final BufferedHttpServletResponse response) throws JSONException {
        // TODO Auto-generated method stub

    }

    /**
     * The multi-document event is fired if the URI contains the designator and there is a parameter in the request
     * which has the key 'alf_method'.
     * 
     * @see com.surevine.alfresco.audit.listeners.PostAuditEventListener#isEventFired(javax.servlet.http.HttpServletRequest,
     *      java.lang.String)
     */
    @Override
    public boolean isEventFired(final HttpServletRequest request) {

        return (request.getRequestURI().contains(URI_DESIGNATOR) && "delete".equals(request.getParameter("alf_method")));
    }

    @Override
    public List<Auditable> populateAuditItems(final HttpServletRequest request, final HttpServletResponse response) throws JSONException {
        List<Auditable> events = new ArrayList<Auditable>();

        JSONObject json = parseJSONFromPostContent(request);

        if (json != null && json.has(AlfrescoJSONKeys.NODEREFS)) {
            JSONArray nodeRefs = json.getJSONArray(AlfrescoJSONKeys.NODEREFS);

            for (int i = 0; i < nodeRefs.length(); i++) {
                String nodeStr = nodeRefs.getString(i);

                if (nodeStr != null) {
                    Auditable event = new AuditItem();

                    setGenericAuditMetadata(event, request);
                    setMetadataFromNodeRef(event, nodeRefResolver.getNodeRefFromGUID(nodeStr));

                    // Now add the populated event to the array
                    events.add(event);

                }
            }
        }

        return events;
    }
}
