package org.lastbamboo.common.sip.proxy;

import java.io.IOException;

/**
 * Interface for a SIP proxy.
 */
public interface SipProxy
    {

    /**
     * Starts the proxy.
     * @throws IOException If there's an error binding to the port. 
     */
    void start() throws IOException;

    }
