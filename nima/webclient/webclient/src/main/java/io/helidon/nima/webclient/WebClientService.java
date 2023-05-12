package io.helidon.nima.webclient;

/**
 * Extension that can modify web client behavior.
 * This is now only designed for HTTP/1
 */
@FunctionalInterface
public interface WebClientService {
    /**
     * Invoke a service, call {@link Chain#proceed(io.helidon.nima.webclient.WebClientServiceRequest)} to call the
     * next service in the chain.
     *
     * @param chain to invoke next web client service, or the HTTP call if this is the last service
     * @param clientRequest request from the client, or previous services
     * @return response to be returned to the client
     */
    WebClientServiceResponse handle(Chain chain, WebClientServiceRequest clientRequest);

    /**
     * Chain of services.
     */
    interface Chain {
        /**
         * Proceed with invocation of the next service, or the HTTP call.
         * This method is always fully blocking.
         *
         * @param clientRequest request
         * @return response from the next service or HTTP call
         */
        WebClientServiceResponse proceed(WebClientServiceRequest clientRequest);
    }
}
