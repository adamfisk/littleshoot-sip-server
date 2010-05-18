package org.lastbamboo.common.sip.proxy.stateless;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.sip.proxy.SipRegistrar;
import org.lastbamboo.common.sip.proxy.SipRequestAndResponseForwarder;
import org.lastbamboo.common.sip.proxy.SipRequestForwarder;
import org.lastbamboo.common.sip.stack.message.Invite;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageUtils;
import org.lastbamboo.common.sip.stack.message.SipResponse;
import org.lastbamboo.common.sip.stack.message.header.SipHeader;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderNames;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderValue;
import org.lastbamboo.common.sip.stack.transport.SipTcpTransportLayer;
import org.lastbamboo.common.sip.stack.util.UriUtils;

/**
 * Creates a new stateless SIP proxy.
 */
public class StatelessSipProxy implements SipRequestAndResponseForwarder
    {

    private final Logger LOG = LoggerFactory.getLogger(StatelessSipProxy.class);
    
    private final SipRegistrar m_registrar;
    
    private final SipRequestForwarder m_unregisteredUriForwarder;

    private final SipRequestForwarder m_externalDomainForwarder;

    private final UriUtils m_uriUtils;

    private final SipTcpTransportLayer m_transportLayer;

    private final SipMessageFactory m_messageFactory;

    /**
     * Creates a new stateless SIP proxy.
     * 
     * @param transportLayer The class for sending messages.
     * @param registrar The registrar the proxy uses to lookup client 
     * connections.
     * @param unregisteredUriForwarder The class for forwarding messages when
     * we do not have registration data for the URI.
     * @param externalDomainForwarder The class for forwarding messages to
     * domains we are not responsible for, such as 'vonage.com'.
     * @param uriUtils Class for handling SIP uris.
     * @param messageFactory The class for creating SIP messages.
     */
    public StatelessSipProxy(final SipTcpTransportLayer transportLayer, 
        final SipRegistrar registrar, 
        final SipRequestForwarder unregisteredUriForwarder,
        final SipRequestForwarder externalDomainForwarder,
        final UriUtils uriUtils, final SipMessageFactory messageFactory)
        {
        this.m_transportLayer = transportLayer;
        this.m_registrar = registrar;
        this.m_unregisteredUriForwarder = unregisteredUriForwarder;
        this.m_externalDomainForwarder = externalDomainForwarder;
        this.m_uriUtils = uriUtils;
        this.m_messageFactory = messageFactory;
        }
    
    public void forwardSipRequest(final Invite request)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Processing request...");
            }
        // Determine request targets, as specified in RFC 3261 section 16.5.
        
        final URI uri;
        try
            {
            uri = SipMessageUtils.extractUriFromRequestLine(request);
            }
        catch (final IOException e)
            {
            // TODO Return a response indicating an invalid message from the
            // client.  For now, we just drop it.
            LOG.warn("Could not extract URI from request: "+request);
            return;
            }
        
        final String host = this.m_uriUtils.getHostInSipUri(uri);
        if (host.equalsIgnoreCase("lastbamboo.org"))
            {
            // Check our registrar for the user, and forward it if we have the
            // user registered.  Otherwise, send to to the external location
            // service.
            if (this.m_registrar.hasRegistration(uri))
                {
                final IoSession io = this.m_registrar.getIoSession(uri);
                
                if (io == null) 
                    {
                    // This can still happen if we happen to lose a 
                    // connection...
                    LOG.debug("Forwarding request for user not registered " +
                        "with this proxy...");
                    this.m_unregisteredUriForwarder.forwardSipRequest(request);
                    }
                else 
                    {
                    LOG.debug("Forwarding message for client we have...");
                    this.m_transportLayer.writeRequestStatelessly(request, io);
                    }
                }
            else 
                {
                LOG.debug("Forwarding request for user not registered " +
                    "with this proxy...");
                
                // TODO: We don't do this for now.
                //this.m_unregisteredUriForwarder.forwardSipRequest(request);
                }
            }
        else
            {
            // We are not responsible for the domain, so forward it 
            // appropriately.  
            LOG.debug("Forwarding request for external domain: "+host);
            this.m_externalDomainForwarder.forwardSipRequest(request);
            }     
        }

    public void forwardSipResponse(final SipResponse originalResponse) 
        throws IOException
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Forwarding original response: "+originalResponse);
            }
        final SipHeader header = 
            originalResponse.getHeader(SipHeaderNames.VIA);
        final List<SipHeaderValue> values = header.getValues();
        if (values.size() < 2)
            {
            LOG.warn("Not enough Via headers in response: "+
                originalResponse);
            throw new IOException("Not enough Via headers " +
                "in response: "+originalResponse);
            }

        final SipResponse response = 
            this.m_messageFactory.stripVia(originalResponse);

        this.m_transportLayer.writeResponse(response);
        }
    }
