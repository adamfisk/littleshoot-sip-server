package org.lastbamboo.common.sip.proxy.stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.sip.proxy.SipRequestForwarder;
import org.lastbamboo.common.sip.stack.message.Invite;

/**
 * Message forwarder for forwarding messages to external domains, such as
 * "vonage.com".
 */
public class ExternalDomainForwarder implements SipRequestForwarder
    {

    private static final Log LOG = 
        LogFactory.getLog(ExternalDomainForwarder.class);
    
    public void forwardSipRequest(final Invite request)
        {
        // TODO: Implement this!!
        LOG.warn("Attempting to forward message to external domain: "+request);
        }

    }
