package io.helidon.nima.webclient;

import io.helidon.common.http.ClientRequestHeaders;
import io.helidon.common.context.Context;
import io.helidon.common.uri.UriFragment;
import io.helidon.common.uri.UriQueryWriteable;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import static io.helidon.common.http.Http.*;

/**
 * Request to SPI {@link WebClientService} that supports modification of the outgoing request.
 */
public interface WebClientServiceRequest {
    /**
     * URI helper for this client request.
     *
     * @return URI helper
     */
    UriHelper uri();

    /**
     * Set the URI this client request.
     *
     */
    void uri(UriHelper uri);


    /**
     * Returns an HTTP request method. See also {@link Method HTTP standard methods} utility class.
     *
     * @return an HTTP method
     * @see Method
     */
    Method method();

    /**
     * Returns an HTTP version from the request line.
     * <p>
     * See {@link Version HTTP Version} enumeration for supported versions.
     * <p>
     * If communication starts as a {@code HTTP/1.1} with {@code h2c} upgrade, then it will be automatically
     * upgraded and this method returns {@code HTTP/2.0}.
     *
     * @return an HTTP version
     */
    Version version();

    /**
     * Returns query parameters.
     *
     * @return an parameters representing query parameters
     */
    UriQueryWriteable query();

    /**
     * Returns a decoded request URI fragment without leading hash '#' character.
     *
     * @return a decoded URI fragment
     */
    UriFragment fragment();

    /**
     * Configured request headers.
     *
     * @return headers (mutable)
     */
    ClientRequestHeaders headers();

    /**
     * Registry that can be used to propagate information from server (e.g. security context, tracing spans etc.).
     *
     * @return registry propagated by the user
     */
    Context context();

    /**
     * Request id which will be used in logging messages.
     *
     * @return current request id
     */
    String requestId();

    /**
     * Set new request id. This id is used in logging messages.
     *
     * @param requestId new request id
     */
    void requestId(String requestId);

    /**
     * Completes when the request part of this request is done (e.g. we have sent all headers and bytes).
     *
     * @return completion stage that finishes when we fully send request (including entity) to server
     */
    CompletionStage<WebClientServiceRequest> whenSent();

    /**
     * Completes when the full processing of this request is done (e.g. we have received a full response).
     *
     * @return completion stage that finishes when we receive and fully read response from the server
     */
    CompletionStage<WebClientServiceResponse> whenComplete();

    /**
     * Properties configured by user when creating this client request.
     *
     * @return properties that were configured (mutable)
     */
    Map<String, String> properties();

    /**
     * Set the new fragment of the request (decoded).
     *
     * @param fragment new request fragment
     */
    void fragment(String fragment);
}
