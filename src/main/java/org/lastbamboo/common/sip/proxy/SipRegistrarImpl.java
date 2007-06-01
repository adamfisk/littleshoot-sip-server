package org.lastbamboo.common.sip.proxy;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFilter.NextFilter;
import org.lastbamboo.common.protocol.CloseListener;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.sip.stack.message.Register;
import org.lastbamboo.common.sip.stack.message.SipMessage;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageUtils;
import org.lastbamboo.common.sip.stack.message.SipResponse;
import org.lastbamboo.common.sip.stack.message.header.SipHeader;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderNames;
import org.lastbamboo.common.sip.stack.transport.SipTcpTransportLayer;
import org.lastbamboo.common.util.MapUtils;

/**
 * Registrar for SIP clients.
 * 
 * TODO: Also create a map of reader/writers to SIP URIs for more efficient
 * removals??
 */
public class SipRegistrarImpl implements SipRegistrar 
    {

    private static final Log LOG = LogFactory.getLog(SipRegistrarImpl.class);
    
    private final SipMessageFactory m_messageFactory;

    private final SipTcpTransportLayer m_transportLayer;
    
    private final Map<URI, IoSession> m_registrations = 
        new ConcurrentHashMap<URI, IoSession>();

    private final Collection<RegistrationListener> m_registrationListeners =
        new LinkedList<RegistrationListener>();

    /**
     * Creates a new registrar.
     * 
     * @param factory The factory for creating messages for responding to
     * register requests.
     * @param transportLayer The transport layer for actually sending data.
     */
    public SipRegistrarImpl(final SipMessageFactory factory, 
        final SipTcpTransportLayer transportLayer)
        {
        this.m_messageFactory = factory;
        this.m_transportLayer = transportLayer;
        }
    
    /*
    public void handleRegister(final Register register, 
        final ReaderWriter readerWriter)
        {
        LOG.debug("Processing registration...");
        readerWriter.addCloseListener(this);
        
        
        // Add a mapping at the transport layer.
        //this.m_transportLayer.addConnection(readerWriter);
        
        // We also need to add a mapping according to the URI.
        final SipHeader fromHeader = register.getHeader(SipHeaderNames.FROM);
        final URI uri = SipMessageUtils.extractUri(fromHeader);
        this.m_registrations.put(uri, readerWriter);
        
        final SipMessage response = 
            this.m_messageFactory.createRegisterOk(register);
        
        final InetSocketAddress remoteAddress = 
            readerWriter.getRemoteSocketAddress();
        LOG.debug("Writing OK response to SIP client...");
        
        this.m_transportLayer.writeResponse(remoteAddress, response);
        notifyListeners(uri, true);
        }
        */
    
    public void handleRegister(final Register register, final IoSession session)
        {
        LOG.debug("Processing registration...");
        //readerWriter.addCloseListener(this);
        
        // We also need to add a mapping according to the URI.
        final SipHeader fromHeader = register.getHeader(SipHeaderNames.FROM);
        final URI uri = SipMessageUtils.extractUri(fromHeader);
        this.m_registrations.put(uri, session);
        
        final SipResponse response = 
            this.m_messageFactory.createRegisterOk(register);
        
        final InetSocketAddress remoteAddress = 
            (InetSocketAddress) session.getRemoteAddress();
        LOG.debug("Writing OK response to SIP client...");
        
        this.m_transportLayer.writeResponse(remoteAddress, response);
        notifyListeners(uri, true);
        }

    public IoSession getIoSession(final URI uri)
        {
        return this.m_registrations.get(uri);
        }

    public boolean hasRegistration(final URI uri)
        {
        return this.m_registrations.containsKey(uri);
        }

    public void sessionClosed(final IoSession session) 
        {
        final URI uri = 
            (URI) MapUtils.removeFromMapValues(this.m_registrations, 
                session);
        if (uri != null)
            {
            notifyListeners(uri, false);
            }
        else
            {
            // Maybe we've received duplicate close events?  Strange.
            LOG.warn("Could not locate URI for reader/writer: " + 
                session + " " + this.m_registrations.keySet());
            }
        }
    
    private void notifyListeners(final URI uri, final boolean registered)
        {
        // Note we're still on the selector thread, so none of the listeners
        // can do anything time consuming at all.  If they do, they need to
        // do it on a separate thread.
        synchronized (this.m_registrationListeners)
            {
            for (final RegistrationListener rl : this.m_registrationListeners)
                {
                if (registered)
                    {
                    rl.onRegistered(uri);
                    }
                else
                    {
                    rl.onUnregistered(uri);
                    }
                }
            }
        }

    public void addRegistrationListener(final RegistrationListener listener)
        {
        LOG.debug("Adding registration listener...");
        this.m_registrationListeners.add(listener);
        }

    }
