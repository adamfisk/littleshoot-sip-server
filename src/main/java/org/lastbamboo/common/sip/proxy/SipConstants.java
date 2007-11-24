package org.lastbamboo.common.sip.proxy;

/**
 * Constants for SIP.
 */
public class SipConstants
    {

    /**
     * Listening port.  OK, this is crazy, but we don't use 5060 because some
     * ISPs, particularly in India and Pakistan but likely elsewhere, block
     * SIP!  Took me awhile to figure that one out from my hotel room in Delhi.
     */
    public static final int SIP_PORT = 5061;

    }
