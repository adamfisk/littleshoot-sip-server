package org.lastbamboo.common.sip.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.sip.stack.message.Invite;
import org.lastbamboo.common.sip.stack.message.OkResponse;
import org.lastbamboo.common.sip.stack.message.Register;
import org.lastbamboo.common.sip.stack.message.RequestTimeoutResponse;
import org.lastbamboo.common.sip.stack.message.SipMessage;
import org.lastbamboo.common.sip.stack.message.SipMessageFactory;
import org.lastbamboo.common.sip.stack.message.SipMessageVisitor;
import org.lastbamboo.common.sip.stack.message.UnknownMessage;
import org.lastbamboo.common.sip.stack.message.header.SipHeader;
import org.lastbamboo.common.sip.stack.message.header.SipHeaderNames;

/**
 * Visitor for visiting SIP message on the proxy.  This delegates to various
 * message processing modules, such as to the SIP registrar or to the 
 * stateless proxy.  This is only for a single client connection.
 */
public class SipProxyMessageVisitor implements SipMessageVisitor
    {

    private static final Log LOG = 
        LogFactory.getLog(SipProxyMessageVisitor.class);
    
    private final SipRegistrar m_registrar;
    private final ReaderWriter m_readerWriter;
    private final SipRequestAndResponseForwarder m_proxy;
    private final InetSocketAddress m_remoteSocketAddress;

    private final SipMessageFactory m_messageFactory;
    
    /**
     * Creates a new class for visiting SIP messages received on the proxy
     * for a single SIP client.
     * 
     * @param readerWriter The reader/writer for reading and writing data
     * from and to the client.
     * @param registrar The registrar for looking up the contact information
     * for clients to forward messages to.
     * @param proxy The stateless proxy for forwarding messages.
     * @param messageFactory Factory for creating new SIP messages.
     */
    public SipProxyMessageVisitor(final ReaderWriter readerWriter, 
        final SipRegistrar registrar, 
        final SipRequestAndResponseForwarder proxy, 
        final SipMessageFactory messageFactory)
        {
        this.m_readerWriter = readerWriter;
        this.m_remoteSocketAddress = readerWriter.getRemoteSocketAddress();
        this.m_registrar = registrar;
        this.m_proxy = proxy;
        this.m_messageFactory = messageFactory;
        }

    public void visitOk(final OkResponse response)
        {
        LOG.debug("Visiting OK response...");
        
        try
            {
            this.m_proxy.forwardSipResponse(response);
            }
        catch (final IOException e)
            {
            LOG.error("Could not process response", e);
            }
        }

    public void visitInvite(final Invite invite)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Visiting invite: "+invite);
            }
        
        final SipHeader via = invite.getHeader(SipHeaderNames.VIA);
        if (via == null)
            {
            LOG.warn("No Via header in INVITE: "+invite);
            // TODO: Return error response to client!!
            return;
            }
        
        final SipMessage inviteToForward;
        try
            {
            inviteToForward = this.m_messageFactory.createInviteToForward(
                this.m_remoteSocketAddress, invite);
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
        
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Forwarding INVITE: "+inviteToForward);
            }
        LOG.debug("Done printing INVITE...");
        this.m_proxy.forwardSipRequest(inviteToForward);
        }

    public void visitRegister(final Register register)
        {
        LOG.debug("Visiting register...");
        this.m_registrar.handleRegister(register, this.m_readerWriter);
        }
    
    public void visitRequestTimedOut(final RequestTimeoutResponse response)
        {
        LOG.warn("Received request timed out on the proxy: "+response);
        }
    
    public void visitUnknownRequest(final UnknownMessage request)
        {
        LOG.warn("Visiting and ignoring unknown request: "+request);
        //this.m_proxy.forwardSipRequest(request);
        }
    }
