package org.lastbamboo.common.sip.proxy;

import java.net.URI;
import java.util.Collection;

import org.apache.mina.common.IoSession;
import org.lastbamboo.common.sip.stack.message.Register;

/**
 * Registrar for SIP clients.
 */
public interface SipRegistrar
    {

    /**
     * Processes the specified register request.
     * 
     * @param register The register request to process.
     * @param io The class for writing message back to the client.
     */
    void handleRegister(Register register, IoSession io);

    /**
     * Accesses the reader/writer for sending a message to the specified URI.
     * 
     * @param uri The URI to send a message to.
     * @return The reader/writer for the specified URI, or <code>null</code>
     * if we don't have information about the URI.
     */
    IoSession getIoSession(URI uri);
    
    /**
     * Called when an IO session has closed -- we have lost a connection to a 
     * client.
     * 
     * @param session The closed session.
     */
    void sessionClosed(IoSession session);

    /**
     * Determines whether or not we have a registration for the specified
     * SIP URI.
     * 
     * @param uri The SIP URI to check for a registration for.
     * @return <code>true</code> if we have a registration for the URI,
     * otherwise <code>false</code>.
     */
    boolean hasRegistration(final URI uri);

    /**
     * Adds the specified listener for registration events.
     * 
     * @param listener The listener to add.
     */
    void addRegistrationListener(RegistrationListener listener);
    
    /**
     * Get registered URIs
     * 
     * @return The registered URIs.
     */
    Collection<URI> getRegistered();

    }
