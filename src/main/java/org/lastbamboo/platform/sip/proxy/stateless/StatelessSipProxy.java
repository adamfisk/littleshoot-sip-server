package org.lastbamboo.platform.sip.proxy.stateless;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.platform.sip.proxy.SipRegistrar;
import org.lastbamboo.platform.sip.proxy.SipRequestAndResponseForwarder;
import org.lastbamboo.platform.sip.proxy.SipRequestForwarder;
import org.lastbamboo.platform.sip.stack.message.SipMessage;
import org.lastbamboo.platform.sip.stack.message.SipMessageFactory;
import org.lastbamboo.platform.sip.stack.message.SipMessageUtils;
import org.lastbamboo.platform.sip.stack.message.header.SipHeader;
import org.lastbamboo.platform.sip.stack.message.header.SipHeaderNames;
import org.lastbamboo.platform.sip.stack.transport.SipTcpTransportLayer;
import org.lastbamboo.platform.sip.stack.util.UriUtils;
import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.protocol.WriteData;
import org.lastbamboo.shoot.protocol.WriteListener;

/**
 * Creates a new stateless SIP proxy.
 */
public class StatelessSipProxy implements SipRequestAndResponseForwarder, 
    WriteListener
    {

    private static final Log LOG = 
        LogFactory.getLog(StatelessSipProxy.class);
    
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
    
    public void forwardSipRequest(final SipMessage request)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Processing request: "+request.getMethod());
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
                final ReaderWriter readerWriter = 
                    this.m_registrar.getReaderWriter(uri);
                
                if (readerWriter == null) 
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
                    this.m_transportLayer.writeRequestStatelessly(request, 
                        readerWriter);
                    }
                }
            else 
                {
                LOG.debug("Forwarding request for user not registered " +
                    "with this proxy...");
                this.m_unregisteredUriForwarder.forwardSipRequest(request);
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

    public void forwardSipResponse(final SipMessage originalResponse) 
        throws IOException
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Processing message: "+originalResponse);
            }
        final SipHeader header = 
            originalResponse.getHeader(SipHeaderNames.VIA);
        final List values = header.getValues();
        if (values.size() < 2)
            {
            LOG.warn("Not enough Via headers in response: "+
                originalResponse);
            throw new IOException("Not enough Via headers " +
                "in response: "+originalResponse);
            }

        final SipMessage response = 
            this.m_messageFactory.stripVia(originalResponse);

        this.m_transportLayer.writeResponse(response);
        }

    public void onWrite(final WriteData data)
        {
        if (LOG.isDebugEnabled())
            {
            final long time = System.currentTimeMillis()-data.getStartTime();
            LOG.debug("Completed write of "+data.getTotalBytes()+" bytes in "+
                time + " milliseconds...");
            if (time > 4000)
                {
                LOG.warn("Writing took "+time+" milliseconds..." + 
                    data.getNumQueued()+" messages queued...");
                }
            }
        }

    }
