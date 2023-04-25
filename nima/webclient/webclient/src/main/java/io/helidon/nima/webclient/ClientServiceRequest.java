package io.helidon.nima.webclient;

import io.helidon.common.http.ClientRequestHeaders;
import io.helidon.common.context.Context;
import io.helidon.nima.webclient.http1.Http1ClientRequest;

public interface ClientServiceRequest extends Http1ClientRequest {

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
    long requestId();

    /**
     * Set new request id. This id is used in logging messages.
     *
     * @param requestId new request id
     */
    void requestId(long requestId);

//    /**
//     * Completes when the request part of this request is done (e.g. we have sent all headers and bytes).
//     *
//     * @return completion stage that finishes when we fully send request (including entity) to server
//     */
//    Single<WebClientServiceRequest> whenSent();
//
//    /**
//     * Completes when the response headers has been received, but entity has not been processed yet.
//     *
//     * @return completion stage that finishes when we received headers
//     */
//    Single<WebClientServiceResponse> whenResponseReceived();
//
//    /**
//     * Completes when the full processing of this request is done (e.g. we have received a full response).
//     *
//     * @return completion stage that finishes when we receive and fully read response from the server
//     */
//    Single<WebClientServiceResponse> whenComplete();

    /**
     * Schema of the request uri.
     * <p>
     * This will not match schema returned by {@link ClientRequest#resolvedUri()}
     * if changed by {@link #schema(String schema)}
     *
     * @return schema of the request
     */
    String schema();

    /**
     * Set new schema of the request.
     *
     * @param schema new request schema
     */
    void schema(String schema);

    /**
     * Host of the request uri.
     * <p>
     * This will not match host returned by {@link ClientRequest#resolvedUri()}
     * if changed by {@link #host(String host)}
     *
     * @return host of the request
     */
    String host();

    /**
     * Set new host of the request.
     *
     * @param host new request host
     */
    void host(String host);

    /**
     * Port of the request uri.
     * <p>
     * This will not match port returned by {@link ClientRequest#resolvedUri()}
     * if changed by {@link #port(int port)}
     *
     * @return port of the request
     */
    int port();

    /**
     * Set new port of the request.
     *
     * @param port new request port
     */
    void port(int port);

    /**
     * Set the new fragment of the request.
     *
     * @param fragment new request fragment
     */
    void fragment(String fragment);
}
