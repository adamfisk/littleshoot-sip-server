package org.lastbamboo.common.sip.proxy;

import org.lastbamboo.common.sip.stack.message.SipMessage;

/**
 * Interface for classes that forward SIP requests.
 */
public interface SipRequestForwarder
    {

    /**
     * Forwards the SIP request to the appropriate target.
     * 
     * @param request The request to forward.
     */
    void forwardSipRequest(final SipMessage request);
    
    }
