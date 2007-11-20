package org.lastbamboo.common.sip.proxy;

/**
 * MBean interface for using JMX to access information about the SIP server. 
 */
public interface SipRegistrarImplMBean
    {

    /**
     * Accessor for the number of registered SIP clients.
     * 
     * @return The number of registered SIP clients.
     */
    int getNumRegistered();
    
    /**
     * Accessor for the maximum number of registrations we've seen.
     * 
     * @return The maximum number of registrations we've seen.
     */
    int getMaxRegistered();
    }
