package org.lastbamboo.common.sip.proxy;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.netmap.IntUriNetMap;
import org.lastbamboo.platform.sip.stack.util.UriUtils;

/**
 * A location service for dealing with Last Bamboo SIP URIs.
 */
public final class LastBambooLocationService implements LocationService
    {
    /**
     * The log for this class.
     */
    private final Log LOG =
            LogFactory.getLog (LastBambooLocationService.class);

    /**
     * URI utilities.
     */
    private final UriUtils m_uriUtils;

    /**
     * The mapping from Last Bamboo user identifiers to the SIP proxies with
     * which they are registered.
     */
    private final IntUriNetMap m_idToProxies;

    /**
     * Constructs a new location service.
     *
     * @param uriUtils
     *      URI utilities.
     * @param idToProxies
     *      The mapping from Last Bamboo SIP URIs to the SIP proxies with which
     *      they are registered.
     */
    public LastBambooLocationService
            (final UriUtils uriUtils,
             final IntUriNetMap idToProxies)
        {
        m_uriUtils = uriUtils;
        m_idToProxies = idToProxies;
        }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle
            (final URI requestUri)
        {
        // We handle any request URI whose host is lastbamboo.org.

        LOG.debug ("requestUri: " + requestUri);

        return (hasDomain (this.m_uriUtils.getHostInSipUri(requestUri)));
        }

    /**
     * {@inheritDoc}
     */
    public Collection getTargetSet
            (final URI requestUri)
        {
        final int personId = m_uriUtils.getPersonIdInSipUri (requestUri);

        final Collection targetSet = new LinkedList ();
        final Iterator proxies = m_idToProxies.getValueIterator (personId);

        while (proxies.hasNext () && targetSet.isEmpty ())
            {
            final URI targetUri = (URI) proxies.next ();
            
            LOG.debug("Accessing host in SIP URI: "+targetUri);
            
            // TODO: Who is to say we are "lastbamboo.org"?
            if (hasDomain (this.m_uriUtils.getHostInSipUri(targetUri)))
                {
                // Do nothing.  We do not forward back to ourselves.
                }
            else
                {
                targetSet.add (targetUri);
                }
            }

        return (targetSet);
        }

    private boolean hasDomain(final String hostInSipUri)
        {
        return (hostInSipUri.equalsIgnoreCase("lastbamboo.org"));
        }
    }
