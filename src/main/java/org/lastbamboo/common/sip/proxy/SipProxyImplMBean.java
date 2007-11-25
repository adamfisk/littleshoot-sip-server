package org.lastbamboo.common.sip.proxy;


/**
 * MBean for the SIP proxy server.
 */
public interface SipProxyImplMBean
    {

    /**
     * Accessor for the port SIP is running on.
     * 
     * @return The port SIP is running on.
     */
    int getSipPort();
    
    }
