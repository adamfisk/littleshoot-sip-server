package org.lastbamboo.common.sip.proxy;

import java.net.URI;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * A chain of location services, any one of which might handle a particular
 * URI.
 */
public final class LocationServiceChain implements LocationService
    {
    /**
     * The log for this class.
     */
    private final Log LOG =
            LogFactory.getLog (LocationServiceChain.class);

    /**
     * The collection that is the chain of location services.  The first
     * element in the collection is the first in the chain.
     */
    private final Collection m_services;

    /**
     * Constructs a new location service chain.
     */
    public LocationServiceChain
            (final Collection services)
        {
        m_services = services;
        }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle
            (final URI requestUri)
        {
        final Predicate predicate =
                new CanHandleRequestPredicate (requestUri);

        return (CollectionUtils.exists (m_services, predicate));
        }

    /**
     * {@inheritDoc}
     */
    public Collection getTargetSet
            (final URI requestUri)
        {
        final Predicate predicate =
                new CanHandleRequestPredicate (requestUri);

        final LocationService service =
                (LocationService) CollectionUtils.find (m_services, predicate);

        Assert.notNull (service);

        return (service.getTargetSet (requestUri));
        }
    }
