package org.lastbamboo.common.sip.proxy;

import java.net.URI;
import java.util.Collection;

/**
 * A location service that provides targets given a request URI.
 */
public interface LocationService
    {
    /**
     * Returns whether this location service can handle a request URI.
     *
     * @param requestUri
     *      The request URI that this location service could potentially
     *      handle.
     *
     * @return
     *      True if this location service can handle the given request URI.
     */
    boolean canHandle
            (URI requestUri);

    /**
     * Returns a target set (URIs) for a given request URI.
     *
     * @param requestUri
     *      The request URI for which to return a target set.
     *
     * @return
     *      A target set (URIs) for a given request URI.
     */
    Collection getTargetSet
            (URI requestUri);
    }
