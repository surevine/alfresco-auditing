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
package com.surevine.alfresco.audit.integration;

import org.json.JSONObject;
import org.junit.Test;

import com.surevine.alfresco.audit.AlfrescoJSONKeys;
import com.surevine.alfresco.audit.Auditable;
import com.surevine.alfresco.audit.listeners.RevertDocumentVersionAuditEventListener;

/**
 * @author garethferrier
 * 
 */

public class RevertDocumentVersionTest extends AbstractAuditIntegrationTestBase {

    /**
     * Constructor.
     */
    public RevertDocumentVersionTest() {
        super(new RevertDocumentVersionAuditEventListener());
    }

    /**
     * Test sunny day scenario.
     */
    @Test
    public void testReversionOfDocument() {

        try {
            JSONObject json = new JSONObject();

            json.put(AlfrescoJSONKeys.NODEREF, TEST_FILENODEREF_STRING);
            json.put(AlfrescoJSONKeys.VERSION, "1.0");
            json.put(AlfrescoJSONKeys.MAJOR_VERSION, "false");
            json.put(AlfrescoJSONKeys.DESCRIPTION, "Reverting");
            mockRequest
                    .setRequestURI("/alfresco/s/api/revert");
            mockRequest.setContent(json.toString().getBytes());

            springAuditFilterBean.doFilter(mockRequest, mockResponse, mockChain);

            Auditable audited = getSingleAuditedEvent();

            assertEquals(TEST_REMOTE_ADDR, audited.getRemoteAddress());
            assertEquals(TEST_FILENODEREF_STRING, audited.getNodeRef());
            assertEquals(TEST_USER, audited.getUser());
            assertEquals(cut.getAction(), audited.getAction());
            assertEquals(TEST_FILE, audited.getSource());

            assertEquals(true, audited.isSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
