package io.helidon.nima.webclient;


/**
 * Client service.
 */
@FunctionalInterface
public interface ClientService {

    ClientServiceRequest request(ClientServiceRequest request);

    default ClientServiceResponse response(ClientServiceRequest request, ClientServiceResponse response) {
        return response;
    }
}
