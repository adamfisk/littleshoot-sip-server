package org.lastbamboo.common.sip.proxy;

import java.net.URI;

/**
 * Interface for classes listening to SIP registration events.
 */
public interface RegistrationListener
    {

    /**
     * Called when the specified SIP URI has registered.
     * 
     * @param uri The SIP URI of the registered user.
     */
    void onRegistered(URI uri);

    /**
     * Called when the specified SIP URI is no longer registered.
     * 
     * @param uri The SIP URI of the registered user.
     */
    void onUnregistered(URI uri);

    }
