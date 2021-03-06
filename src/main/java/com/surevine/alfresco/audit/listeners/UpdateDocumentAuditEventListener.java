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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * @author garethferrier
 * 
 */
public class UpdateDocumentAuditEventListener extends PostFormAuditEventListener {
    Logger logger = Logger.getLogger(UpdateDocumentAuditEventListener.class);
    
    /**
     * Part of the URI that distinguishes event.
     */
    public static final String URI_DESIGNATOR = "api/upload";
    
    /**
     * The string action which identifies the event.
     */
    public static final String ACTION = "DOCUMENT_UPDATED";

    /**
     * Default constructor which provides statics to the super class.
     */
    public UpdateDocumentAuditEventListener() {
        super(URI_DESIGNATOR, ACTION, METHOD);
    }

    @Override
    public boolean isEventFired(final HttpServletRequest request) {
        if(super.isEventFired(request)) {
            FileItemStreamWithValue formItem = formItems.get("updateNodeRef");
            
            if((formItem != null) && formItem.getValue().startsWith("workspace")) {
                return true;
            }
        }
        
        return false;
    }
}
