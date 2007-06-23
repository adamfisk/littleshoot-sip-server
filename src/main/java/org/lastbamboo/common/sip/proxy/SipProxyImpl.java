package org.lastbamboo.common.sip.proxy;

import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.lastbamboo.common.sip.stack.codec.SipIoHandler;
import org.lastbamboo.common.sip.stack.codec.SipProtocolCodecFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitorFactory;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderFactory;
import org.lastbamboo.common.sip.stack.transport.SipTcpTransportLayer;
import org.lastbamboo.common.util.mina.MinaTcpServer;

/**
 * Implementation of a SIP proxy.
 */
public class SipProxyImpl implements SipProxy, IoServiceListener
    {
    
    private static final Log LOG = LogFactory.getLog(SipProxyImpl.class);

    private final SipMessageFactory m_sipMessageFactory;
    
    private final SipRequestAndResponseForwarder m_forwarder;
    private final SipRegistrar m_registrar;
    
    /**
     * Use the default SIP port.
     */
    private static final int SIP_PORT = 5060;

    private final SipTcpTransportLayer m_transportLayer;

    private final SipHeaderFactory m_sipHeaderFactory;

    private MinaTcpServer m_minaServer;

    /**
     * Creates a new SIP server.
     * 
     * @param forwarder The class that forwards messages.
     * @param registrar The class that tracks registered clients.
     * @param sipHeaderFactory The class for creating SIP headers.
     * @param sipMessageFactory The class for creating SIP messages.
     * @param transportLayer The class that writes messages to the network,
     * modifying them as appropriate prior to transport.
     */
    public SipProxyImpl(
        final SipRequestAndResponseForwarder forwarder,
        final SipRegistrar registrar,
        final SipHeaderFactory sipHeaderFactory,
        final SipMessageFactory sipMessageFactory,
        final SipTcpTransportLayer transportLayer)
        {
        m_forwarder = forwarder;
        m_registrar = registrar;
        m_sipHeaderFactory = sipHeaderFactory;
        m_sipMessageFactory = sipMessageFactory;
        m_transportLayer = transportLayer;
        
        // Configure the MINA buffers for optimal performance.
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        final ProtocolCodecFactory codecFactory = 
            new SipProtocolCodecFactory(m_sipHeaderFactory);
        
        final SipMessageVisitorFactory visitorFactory = 
            new SipProxyMessageVisitorFactory(m_forwarder, m_registrar, 
                m_sipMessageFactory);
        final IoHandler handler = new SipIoHandler(visitorFactory);
        this.m_minaServer = new MinaTcpServer(codecFactory, this, handler, 
            SIP_PORT);
        }

    public void start()
        {
        this.m_minaServer.start();
        }

    public void sessionCreated(final IoSession session)
        {
        this.m_transportLayer.addConnection(session);
        }

    public void sessionDestroyed(final IoSession session)
        {
        LOG.debug("Session was destroyed!!");
        this.m_registrar.sessionClosed(session);
        this.m_transportLayer.removeConnection(session);
        }

    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        LOG.debug("Service activated!! "+service);
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        LOG.debug("Service deactivated!! "+service);
        }
    }
