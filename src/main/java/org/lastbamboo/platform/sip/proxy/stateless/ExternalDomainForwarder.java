package org.lastbamboo.platform.sip.proxy.stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.platform.sip.proxy.SipRequestForwarder;
import org.lastbamboo.platform.sip.stack.message.SipMessage;

/**
 * Message forwarder for forwarding messages to external domains, such as
 * "vonage.com".
 */
public class ExternalDomainForwarder implements SipRequestForwarder
    {

    private static final Log LOG = 
        LogFactory.getLog(ExternalDomainForwarder.class);
    
    public void forwardSipRequest(final SipMessage request)
        {
        // TODO: Implement this!!
        LOG.warn("Attempting to forward message to external domain: "+request);
        }

    }
