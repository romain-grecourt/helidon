package io.helidon.nima.webclient;

import io.helidon.builder.Builder;
import io.helidon.common.buffers.DataReader;
import io.helidon.common.http.ClientResponseHeaders;
import io.helidon.common.http.Http;

import java.util.concurrent.CompletableFuture;

/**
 * Response which is created upon receiving of server response.
 */
@Builder
public interface WebClientServiceResponse {

    /**
     * Received response headers.
     *
     * @return immutable response headers
     */
    ClientResponseHeaders headers();

    /**
     * Status of the response.
     *
     * @return response status
     */
    Http.Status status();

    /**
     * Data reader to obtain response bytes.
     *
     * @return data reader
     */
    DataReader reader();

    /**
     * Client connection that was used to handle this request.
     * This connection will be closed/released once the entity is fully read, depending on keep alive configuration.
     *
     * @return connection
     */
    ClientConnection connection();

    /**
     * Completable future to be completed by the client response when the entity is fully read.
     *
     * @return completable future to be finished by the client response
     */
    CompletableFuture<WebClientServiceResponse> whenComplete();

    /**
     * The service request used to invoke the final call.
     *
     * @return service request
     */
    WebClientServiceRequest serviceRequest();
}