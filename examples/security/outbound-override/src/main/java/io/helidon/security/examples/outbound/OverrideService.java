package io.helidon.security.examples.outbound;

import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.security.SecurityContext;
import io.helidon.security.integration.nima.WebClientSecurity;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

final class OverrideService implements HttpService {

    private final Http1Client client = Http1Client.builder()
                                                  .service(WebClientSecurity.create())
                                                  .build();

    @Override
    public void routing(HttpRules rules) {
        rules.get("/override", this::override)
             .get("/propagate", this::propagate);
    }


    private void override(ServerRequest req, ServerResponse res) {
        SecurityContext context = req.context()
                                     .get(SecurityContext.class)
                                     .orElseThrow(() -> new RuntimeException("Security not configured"));

        String result = client.get("http://localhost:" + req.server().port("backend") + "/hello")
                              .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "jill")
                              .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "anotherPassword")
                              .request(String.class);

        res.send("You are: " + context.userName() + ", backend service returned: " + result + "\n");
    }

    private void propagate(ServerRequest req, ServerResponse res) {
        SecurityContext context = req.context()
                                     .get(SecurityContext.class)
                                     .orElseThrow(() -> new RuntimeException("Security not configured"));

        String result = client.get("http://localhost:" + req.server().port("backend") + "/hello")
                              .request(String.class);

        res.send("You are: " + context.userName() + ", backend service returned: " + result + "\n");
    }

}
