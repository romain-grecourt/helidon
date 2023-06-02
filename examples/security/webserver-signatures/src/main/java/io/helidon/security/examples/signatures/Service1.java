package io.helidon.security.examples.signatures;

import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.security.SecurityContext;
import io.helidon.security.integration.nima.WebClientSecurity;

class Service1 implements HttpService {

    private final Http1Client client = Http1Client.builder()
                                                  .service(WebClientSecurity.create())
                                                  .baseUri("http://localhost:8080")
                                                  .build();

    @Override
    public void routing(HttpRules rules) {
        rules.get("/service1", this::service1)
             .get("/service1-rsa", this::service1Rsa);
    }

    private void service1(ServerRequest req, ServerResponse res) {
        handle(req, res, "/service2");
    }

    private void service1Rsa(ServerRequest req, ServerResponse res) {
        handle(req, res, "/service2-rsa");
    }

    private void handle(ServerRequest req, ServerResponse res, String path) {
        res.headers().contentType(HttpMediaType.PLAINTEXT_UTF_8);
        req.context()
           .get(SecurityContext.class)
           .ifPresentOrElse(context -> {
               try (Http1ClientResponse clientRes = client.get(path).request()) {
                   if (clientRes.status() == Http.Status.OK_200) {
                       res.send(clientRes.entity().as(String.class));
                   } else {
                       res.send("Request failed, status: " + clientRes.status());
                   }
               }
           }, () -> res.send("Security context is null"));
    }
}
