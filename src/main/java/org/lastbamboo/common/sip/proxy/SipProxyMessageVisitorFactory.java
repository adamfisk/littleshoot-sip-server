package org.lastbamboo.common.sip.proxy;

import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitor;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitorFactory;

/**
 * Factory for creating SIP proxy message visitors, the class for processing
 * read messages on the server.
 */
public class SipProxyMessageVisitorFactory implements SipMessageVisitorFactory
    {
    
    private final SipRequestAndResponseForwarder m_forwarder;
    private final SipRegistrar m_registrar;
    private final SipMessageFactory m_messageFactory;

    /**
     * Creates a new factory.
     * 
     * @param forwarder The class that forwards messages to their destinations.
     * @param registrar The class that keeps track of registered clients.
     * @param messageFactory The factory for creating SIP messages.
     */
    public SipProxyMessageVisitorFactory(
        final SipRequestAndResponseForwarder forwarder,
        final SipRegistrar registrar,
        final SipMessageFactory messageFactory)
        {
        m_forwarder = forwarder;
        m_registrar = registrar;
        m_messageFactory = messageFactory;
        }

    public SipMessageVisitor createVisitor(final IoSession session)
        {
        return new SipProxyMessageVisitor(m_forwarder, m_registrar, 
            m_messageFactory, session);
        }

    }
