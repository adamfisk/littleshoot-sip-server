package org.lastbamboo.common.sip.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.lastbamboo.common.sip.stack.codec.SipCodecFactory;
import org.lastbamboo.common.sip.stack.codec.SipIoHandler;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitorFactory;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderFactory;
import org.lastbamboo.common.sip.stack.transport.SipTcpTransportLayer;

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
        }

    public void start()
        {
        final Executor threadPool = Executors.newCachedThreadPool();
        final SocketAcceptor acceptor = new SocketAcceptor(
            Runtime.getRuntime().availableProcessors() + 1, threadPool);
        
        acceptor.addListener(this);

        final ProtocolCodecFactory codecFactory = 
            new SipCodecFactory(m_sipHeaderFactory);
        acceptor.getFilterChain().addLast(
            "codec", new ProtocolCodecFilter(codecFactory));

        /*
        acceptor.setReuseAddress(true);
        acceptor.getSessionConfig().setReuseAddress(true);
        acceptor.getSessionConfig().setReceiveBufferSize(1024);
        acceptor.getSessionConfig().setSendBufferSize(1024);
        //((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
        //acceptor.getSessionConfig().setSoLinger(-1);
        acceptor.setBacklog(10240);
        
        acceptor.setLocalAddress(new InetSocketAddress(SIP_PORT));
        */
        
        final SipMessageVisitorFactory visitorFactory = 
            new SipProxyMessageVisitorFactory(m_forwarder, m_registrar, 
                m_sipMessageFactory);
        
        final IoHandler handler = new SipIoHandler(visitorFactory);
        
        //acceptor.setHandler(handler);
        
        try
            {
            acceptor.bind(new InetSocketAddress(SIP_PORT), handler);
            }
        catch (final IOException e)
            {
            LOG.error("Could not bind!!", e);
            }
        }

    public void serviceActivated(final IoService service)
        {
        LOG.debug("Service activated!! "+service);
        }

    public void serviceDeactivated(final IoService service)
        {
        LOG.debug("Service deactivated!! "+service);
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
