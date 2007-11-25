package org.lastbamboo.common.sip.proxy;

import java.lang.management.ManagementFactory;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.lastbamboo.common.sip.stack.codec.SipIoHandler;
import org.lastbamboo.common.sip.stack.codec.SipProtocolCodecFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitorFactory;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderFactory;
import org.lastbamboo.common.sip.stack.transport.SipTcpTransportLayer;
import org.lastbamboo.common.util.JmxUtils;
import org.lastbamboo.common.util.RuntimeIoException;
import org.lastbamboo.common.util.mina.MinaTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a SIP proxy.
 */
public class SipProxyImpl implements SipProxy, IoServiceListener,
    SipProxyImplMBean
    {
    
    private final Logger m_log = LoggerFactory.getLogger(getClass());

    private final SipMessageFactory m_sipMessageFactory;
    
    private final SipRequestAndResponseForwarder m_forwarder;
    private final SipRegistrar m_registrar;

    private final SipTcpTransportLayer m_transportLayer;

    private final SipHeaderFactory m_sipHeaderFactory;

    private final MinaTcpServer m_minaServer;

    private final AtomicBoolean m_serviceActivated = new AtomicBoolean(false);

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

        m_log.debug("Starting server on: " + SipConstants.SIP_PORT);
        
        final ProtocolCodecFactory codecFactory = 
            new SipProtocolCodecFactory(m_sipHeaderFactory);
        
        final SipMessageVisitorFactory visitorFactory = 
            new SipProxyMessageVisitorFactory(m_forwarder, m_registrar, 
                m_sipMessageFactory);
        final IoHandler handler = new SipIoHandler(visitorFactory);
        this.m_minaServer = new MinaTcpServer(codecFactory, this, handler, 
            "SIP-Proxy");
        }

    public void start()
        {
        m_log.debug("Starting MINA server...");
        this.m_minaServer.start(SipConstants.SIP_PORT);
        
        // Wait for the server to really start.
        synchronized (this.m_serviceActivated)
            {
            if (!this.m_serviceActivated.get())
                {
                try
                    {
                    this.m_serviceActivated.wait(6000);
                    }
                catch (final InterruptedException e)
                    {
                    m_log.error("Interrupted??", e);
                    }
                }
            }
        
        if (!this.m_serviceActivated.get())
            {
            m_log.error("Server not started!!");
            throw new RuntimeIoException("Could not start SIP server");
            }
        else
            {
            m_log.debug("Started server...");
            }
        
        // Start this last because otherwise we might be seen as "online"
        // prematurely.
        startJmxServer();
        }

    public void sessionCreated(final IoSession session)
        {
        this.m_transportLayer.addConnection(session);
        }

    public void sessionDestroyed(final IoSession session)
        {
        m_log.debug("Session was destroyed: {}", session);
        this.m_registrar.sessionClosed(session);
        this.m_transportLayer.removeConnection(session);
        }

    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Service activated on: {}", serviceAddress);
        this.m_serviceActivated.set(true);
        synchronized (this.m_serviceActivated)
            {
            this.m_serviceActivated.notify();
            }
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Service deactivated on: "+serviceAddress);
        }
    

    private void startJmxServer()
        {
        m_log.debug("Starting JMX server on: {}",
            System.getProperty("com.sun.management.jmxremote.port"));
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JmxUtils.register(mbs, this.m_registrar);
        JmxUtils.register(mbs, this);
        }
    
    public int getSipPort()
        {
        return SipConstants.SIP_PORT;
        }
    
    @Override
    public String toString()
        {
        return getClass().getSimpleName();
        }
    }
