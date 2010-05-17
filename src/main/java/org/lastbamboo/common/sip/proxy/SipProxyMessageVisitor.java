package org.lastbamboo.common.sip.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.sip.stack.message.DoubleCrlfKeepAlive;
import org.lastbamboo.common.sip.stack.message.Invite;
import org.lastbamboo.common.sip.stack.message.Register;
import org.lastbamboo.common.sip.stack.message.RequestTimeoutResponse;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitor;
import org.lastbamboo.common.sip.stack.message.SipResponse;
import org.lastbamboo.common.sip.stack.message.UnknownSipRequest;
import org.lastbamboo.common.sip.stack.message.header.SipHeader;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message visitor for SIP servers.
 */
public class SipProxyMessageVisitor implements SipMessageVisitor
    {
    
    private static final Logger LOG = 
        LoggerFactory.getLogger(SipProxyMessageVisitor.class);
    private final SipRequestAndResponseForwarder m_forwarder;
    private final SipRegistrar m_registrar;
    private final SipMessageFactory m_messageFactory;
    private final IoSession m_ioSession;
    private volatile static int s_registersVisited = 0;
    private volatile static int s_invitesVisited = 0;
    private volatile static int s_responsesVisited = 0;
    
    /**
     * Creates a new visitor.
     * 
     * @param forwarder The class that forwards messages to other clients.
     * @param registrar The SIP registrar.
     * @param messageFactory The factory for creating new messages and adding
     * and removing Via header values.
     * @param session The session for reading and writing to the original 
     * sender if necessary.
     */
    public SipProxyMessageVisitor(
        final SipRequestAndResponseForwarder forwarder,
        final SipRegistrar registrar,
        final SipMessageFactory messageFactory, final IoSession session)
        {
        m_forwarder = forwarder;
        m_registrar = registrar;
        m_messageFactory = messageFactory;
        m_ioSession = session;
        }

    public void visitResponse(final SipResponse response)
        {
        s_responsesVisited++;
        if (LOG.isDebugEnabled())
            {
            // We know these are OKs to INVITEs because the server will
            // never visit REGISTER OKs.
            LOG.debug("Now visited "+s_responsesVisited+" responses");
            }
        
        try
            {
            this.m_forwarder.forwardSipResponse(response);
            }
        catch (final IOException e)
            {
            LOG.error("Could not process response", e);
            }
        }

    public void visitInvite(final Invite invite)
        {
        s_invitesVisited++;
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Now visited "+s_invitesVisited+" invites");
            }
        
        final SipHeader via = invite.getHeader(SipHeaderNames.VIA);
        if (via == null)
            {
            LOG.warn("No Via header in INVITE: "+invite);
            // TODO: Return error response to client!!
            return;
            }
        
        final InetSocketAddress remoteSocketAddress = 
            (InetSocketAddress) this.m_ioSession.getRemoteAddress();
        
        final Invite inviteToForward;
        try
            {
            inviteToForward = this.m_messageFactory.createInviteToForward(
                remoteSocketAddress, invite);
            }
        catch (final IOException e)
            {
            // TODO Remove the connection??
            LOG.warn("Could not create INVITE to forward..");
            return;
            }  
        if (inviteToForward == null)
            {
            return;
            }
        
        this.m_forwarder.forwardSipRequest(inviteToForward);
        }

    public void visitRegister(final Register register)
        {
        s_registersVisited++;
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Now visited "+s_registersVisited+" registers");
            }
        
        this.m_registrar.handleRegister(register, this.m_ioSession);
        }
    
    public void visitDoubleCrlfKeepAlive(final DoubleCrlfKeepAlive keepAlive)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Visiting double CRLF from: {}", 
                this.m_ioSession.getRemoteAddress());
            }
        
        // TODO: Do nothing for now.  We should return with a single CRLF, as 
        // that would verify to the client that the flow's alive, but that's not
        // really our purpose.  We're just verifying the connection still 
        // exists.
        }
    
    public void visitRequestTimedOut(final RequestTimeoutResponse response)
        {
        LOG.warn("Received request timed out on the proxy: "+response);
        }
    
    public void visitUnknownRequest(final UnknownSipRequest request)
        {
        LOG.warn("Visiting and ignoring unknown request: "+request);
        //this.m_proxy.forwardSipRequest(request);
        }

    }
