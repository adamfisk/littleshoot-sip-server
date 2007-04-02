package org.lastbamboo.platform.sip.proxy;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.nio.AcceptorListener;
import org.lastbamboo.shoot.nio.NioServer;
import org.lastbamboo.shoot.nio.NioServerImpl;
import org.lastbamboo.shoot.nio.SelectorManager;

/**
 * Implementation of a SIP proxy.
 */
public class SipProxyImpl implements SipProxy
    {
    
    private static final Log LOG = LogFactory.getLog(SipProxyImpl.class);
    
    private final SelectorManager m_selectorManager;

    private final AcceptorListener m_acceptorListener;

    private final int m_listeningPort;

    /**
     * Creates a new SIP proxy server.
     * 
     * @param selector The NIO selector class.
     * @param acceptorListener The listener for incoming sockets.
     */
    public SipProxyImpl(final SelectorManager selector, 
        final AcceptorListener acceptorListener)
        {
        this(selector, acceptorListener, 5060);
        }
    
    /**
     * Creates a new SIP proxy server with a custom listening port.
     * 
     * @param selector The NIO selector class.
     * @param acceptorListener The listener for incoming sockets.
     * @param listeningPort The port the server should listen on.
     */
    public SipProxyImpl(final SelectorManager selector, 
        final AcceptorListener acceptorListener, final int listeningPort)
        {
        this.m_selectorManager = selector;
        this.m_acceptorListener = acceptorListener;
        this.m_listeningPort = listeningPort;
        }

    public void start()
        {
        LOG.debug("Starting SIP proxy...");
        final NioServer server = 
            new NioServerImpl(this.m_listeningPort, 
                this.m_selectorManager, this.m_acceptorListener);
        try
            {
            server.startServer();
            }
        catch (final IOException e)
            {
            LOG.fatal("Could not start server", e);
            }
        }

    
    }
