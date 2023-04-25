package io.helidon.nima.webclient;

import io.helidon.common.http.ClientResponseHeaders;
import io.helidon.common.http.Http;
import io.helidon.config.Config;

/**
 * Response which is created upon receiving of server response.
 */
public interface ClientServiceResponse {

    /**
     * Received response headers.
     *
     * @return immutable response headers
     */
    ClientResponseHeaders headers();

    /**
     * Context in which this response is received.
     *
     * @return current context
     */
    Config.Context context();

    /**
     * Status of the response.
     *
     * @return response status
     */
    Http.Status status();

}