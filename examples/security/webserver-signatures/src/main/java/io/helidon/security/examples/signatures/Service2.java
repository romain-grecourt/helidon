package io.helidon.security.examples.signatures;

import io.helidon.common.http.HttpMediaType;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.security.SecurityContext;
import io.helidon.security.Subject;

import java.util.Optional;

class Service2 implements HttpService {

    @Override
    public void routing(HttpRules rules) {
        rules.get("/{*}", this::handle);
    }

    private void handle(ServerRequest req, ServerResponse res) {
        Optional<SecurityContext> securityContext = req.context().get(SecurityContext.class);
        res.headers().contentType(HttpMediaType.PLAINTEXT_UTF_8);
        res.send("Response from service2, you are: \n" + securityContext
                .flatMap(SecurityContext::user)
                .map(Subject::toString)
                .orElse("Security context is null") + ", service: " + securityContext
                .flatMap(SecurityContext::service)
                .map(Subject::toString));
    }
}
