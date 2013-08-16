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
import com.surevine.alfresco.audit.listeners.EditDiscussionReplyAuditEventListener;

/**
 * @author garethferrier
 * 
 */

public class EditDiscussionReplyTest extends AbstractAuditIntegrationTestBase {

    /**
     * Constructor.
     */
    public EditDiscussionReplyTest() {
        super(new EditDiscussionReplyAuditEventListener());
    }

    /**
     * Test sunny day scenario.
     */
    @Test
    public void testSuccess() {

        mockRequest
                .setRequestURI("/alfresco/s/api/forum/post/node/workspace/SpacesStore/" + TEST_DISCUSSION_REPLY_NODEREF_ID);

        try {

            JSONObject json = new JSONObject();
            json.put(AlfrescoJSONKeys.CONTENT, "<p>A reply.&nbsp; And now with an edit.</p>");
            json.put(AlfrescoJSONKeys.SITE, TEST_SITE);
            json.put(AlfrescoJSONKeys.PAGE, "discussions-topicview");
            json.put(AlfrescoJSONKeys.CONTAINER, "discussions");
            eslFixture.addToJSON(json);
            
            mockRequest.setContent(json.toString().getBytes());

            springAuditFilterBean.doFilter(mockRequest, mockResponse, mockChain);
            
            Auditable audited = getSingleAuditedEvent();

            verifyGenericAuditMetadata(audited);

            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }
}
