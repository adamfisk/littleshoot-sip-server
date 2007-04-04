package org.lastbamboo.common.sip.proxy;

import java.net.URI;

import org.apache.commons.collections.Predicate;

/**
 * A predicate that evaluates a location service to see if it can handle a
 * request provided in this class's constructor.
 */
public final class CanHandleRequestPredicate implements Predicate
    {

    /**
     * The request URI that the location could potentially handle.
     */
    private final URI m_requestUri;

    /**
     * Constructs a new predicate.
     *
     * @param requestUri
     *      The request URI that the location could potentially handle.
     */
    public CanHandleRequestPredicate
            (final URI requestUri)
        {
        m_requestUri = requestUri;
        }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate
            (final Object object)
        {
        final LocationService service = (LocationService) object;

        return (service.canHandle (m_requestUri));
        }
    }
