package org.lastbamboo.platform.sip.proxy;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioReaderWriter;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.platform.sip.stack.message.SipMessageFactory;
import org.lastbamboo.platform.sip.stack.message.SipMessageVisitor;
import org.lastbamboo.platform.sip.stack.message.SipProtocolHandler;
import org.lastbamboo.platform.sip.stack.transport.SipTcpTransportLayer;

/**
 * Creates a new listener for accepted sockets to the SIP proxy server.  
 */
public final class SipProxyAcceptorListener implements AcceptorListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(SipProxyAcceptorListener.class);
    
    private final SelectorManager m_selectorManager;

    private final SipMessageFactory m_sipMessageFactory;

    private final SipRegistrar m_sipRegistrar;

    private final SipRequestAndResponseForwarder m_proxy;

    private final SipTcpTransportLayer m_transportLayer;

    /**
     * Creates a new class that listens for incoming connections from SIP
     * clients.
     * 
     * @param manager The class that manages the selector for incoming
     * SIP client messages.
     * @param messageFactory The class that creates new SIP messages.
     * @param registrar The class that handles registrations.
     * @param proxy The SIP proxy.
     * @param transportLayer The transport layer for keeping track of 
     * socket connections.
     */
    public SipProxyAcceptorListener(final SelectorManager manager,
        final SipMessageFactory messageFactory, final SipRegistrar registrar,
        final SipRequestAndResponseForwarder proxy, 
        final SipTcpTransportLayer transportLayer)
        {
        this.m_selectorManager = manager;
        this.m_sipMessageFactory = messageFactory;
        this.m_sipRegistrar = registrar;
        this.m_proxy = proxy;
        this.m_transportLayer = transportLayer;
        }
    
    public void onAccept(final SocketChannel sc)
        {
        LOG.debug("Accepting client....");
        final ReaderWriter readerWriter;
        try
            {
            readerWriter = new NioReaderWriter(sc, this.m_selectorManager);
            }
        catch (final SocketException e)
            {
            handleException(sc, e);
            return;
            }
        catch (final IOException e)
            {
            handleException(sc, e);
            return;
            }

        final SipMessageVisitor visitor = 
            new SipProxyMessageVisitor(readerWriter, this.m_sipRegistrar, 
                this.m_proxy, this.m_sipMessageFactory);
        final ProtocolHandler protocolHandler = 
            new SipProtocolHandler(this.m_sipMessageFactory, visitor);
        readerWriter.setProtocolHandler(protocolHandler);
        
        this.m_transportLayer.addConnection(readerWriter);
        }

    private void handleException(final SocketChannel sc, final IOException e)
        {
        LOG.warn("Unexpected exception on the socket", e);
        try
            {
            sc.close();
            }
        catch (final IOException ioe)
            {
            LOG.warn("Unexpected exception closing socket", ioe);
            }
        }

    }
