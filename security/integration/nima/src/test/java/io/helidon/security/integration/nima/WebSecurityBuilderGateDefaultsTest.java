/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.security.integration.nima;

import java.util.Optional;
import java.util.Set;

import io.helidon.common.context.Contexts;
import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.config.Config;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.webclient.WebClient;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.context.ContextFeature;
import io.helidon.security.AuditEvent;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link SecurityFeature}.
 */
@SuppressWarnings("resource")
@ServerTest
class WebSecurityBuilderGateDefaultsTest {
    private static io.helidon.security.integration.nima.UnitTestAuditProvider myAuditProvider;
    private static WebServer server;
    private static Http1Client securitySetup;
    private static Http1Client webClient;
    private static String serverBaseUri;

    @BeforeAll
    static void setupClients() {
        Security clientSecurity = Security.builder()
                                          .addProvider(HttpBasicAuthProvider.builder().build())
                                          .build();

        securitySetup = WebClient.builder()
                                 .service(ClientSecurity.create(clientSecurity))
                                 .build();

        webClient = WebClient.builder().build();
    }

    @BeforeAll
    static void initClass() {
        WebSecurityTestUtil.auditLogFinest();
        myAuditProvider = new UnitTestAuditProvider();

        Config config = Config.create();

        Security security = Security.builder(config.get("security"))
                                    .addAuditProvider(myAuditProvider).build();

        server = WebServer.builder()
                          .routing(builder -> builder.addFeature(ContextFeature.create())
                                                     .addFeature(SecurityFeature.create(security)
                                                                                .securityDefaults(SecurityFeature.rolesAllowed("admin").audit()))
                                                     // will only accept admin (due to gate defaults)
                                                     .get("/noRoles", SecurityFeature.authenticate())
                                                     .get("/user[/{*}]", SecurityFeature.rolesAllowed("user"))
                                                     .get("/admin", SecurityFeature.rolesAllowed("admin"))
                                                     // will also accept admin (due to gate defaults)
                                                     .get("/deny", SecurityFeature.rolesAllowed("deny"))
                                                     // audit is on from gate defaults
                                                     .get("/auditOnly", SecurityFeature
                                                             .enforce()
                                                             .skipAuthentication()
                                                             .skipAuthorization()
                                                             .auditEventType("unit_test")
                                                             .auditMessageFormat(WebSecurityTests.AUDIT_MESSAGE_FORMAT)
                                                     )
                                                     .get("/{*}", (req, res) -> {
                                                         Optional<SecurityContext> securityContext = Contexts.context()
                                                                                                             .flatMap(it -> it.get(SecurityContext.class));
                                                         res.headers().contentType(HttpMediaType.PLAINTEXT_UTF_8);
                                                         res.send("Hello, you are: \n" + securityContext
                                                                 .map(ctx -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString())
                                                                 .orElse("Security context is null"));
                                                     }))
                          .build();

        server.start();
        serverBaseUri = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stopIt() {
        server.stop();
    }

    @Test
    public void testCustomizedAudit() {
        // even though I send username and password, this MUST NOT require authentication
        // as then audit is called twice - first time with 401 (challenge) and second time with 200 (correct request)
        // and that intermittently breaks this test
        Http1ClientResponse response = webClient.get()
                                                .uri(serverBaseUri + "/auditOnly")
                                                .request();

        assertThat(response.status(), is(Http.Status.OK_200));

        // audit
        AuditEvent auditEvent = myAuditProvider.getAuditEvent();
        assertThat(auditEvent, notNullValue());
        assertThat(auditEvent.toString(), auditEvent.messageFormat(), is(WebSecurityTests.AUDIT_MESSAGE_FORMAT));
        assertThat(auditEvent.toString(), auditEvent.severity(), is(AuditEvent.AuditSeverity.SUCCESS));
    }

    @Test
    void basicTestJohn() {
        String username = "john";
        String password = "password";

        testForbidden(serverBaseUri + "/noRoles", username, password);
        testForbidden(serverBaseUri + "/user", username, password);
        testForbidden(serverBaseUri + "/admin", username, password);
        testForbidden(serverBaseUri + "/deny", username, password);
    }

    @Test
    void basicTestJack() {
        String username = "jack";
        String password = "jackIsGreat";

        testProtected(serverBaseUri + "/noRoles",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testProtected(serverBaseUri + "/user",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testProtected(serverBaseUri + "/admin",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testProtected(serverBaseUri + "/deny",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
    }

    @Test
    void basicTestJill() {
        String username = "jill";
        String password = "password";

        testForbidden(serverBaseUri + "/noRoles", username, password);
        testProtected(serverBaseUri + "/user",
                username,
                password,
                Set.of("user"),
                Set.of("admin"));
        testForbidden(serverBaseUri + "/admin", username, password);
        testForbidden(serverBaseUri + "/deny", username, password);
    }

    @Test
    void basicTest401() {
        // here we call the endpoint
        Http1ClientResponse response = webClient.get()
                                                .uri(serverBaseUri + "/noRoles")
                                                .request();

        if (response.status() != Http.Status.UNAUTHORIZED_401) {
            assertThat("Response received: " + response.entity().as(String.class),
                    response.status(),
                    is(Http.Status.UNAUTHORIZED_401));
        }

        Optional<String> authenticate = response.headers().first(Http.Header.WWW_AUTHENTICATE);
        assertThat(authenticate, optionalValue(is("Basic realm=\"mic\"")));

        Http1ClientResponse webClientResponse = callProtected(serverBaseUri + "/noRoles", "invalidUser", "invalidPassword");
        assertThat(webClientResponse.status(), is(Http.Status.UNAUTHORIZED_401));
        authenticate = webClientResponse.headers().first(Http.Header.WWW_AUTHENTICATE);

        assertThat(authenticate, optionalValue(is("Basic realm=\"mic\"")));
    }

    private void testForbidden(String uri, String username, String password) {
        Http1ClientResponse response = callProtected(uri, username, password);
        assertThat(uri + " for user " + username + " should be forbidden",
                response.status(),
                is(Http.Status.FORBIDDEN_403));
    }

    private void testProtected(String uri,
                               String username,
                               String password,
                               Set<String> expectedRoles,
                               Set<String> invalidRoles) {

        Http1ClientResponse response = callProtected(uri, username, password);
        assertThat(response.status(), is(Http.Status.OK_200));

        String entity = response.entity().as(String.class);

        // check login
        assertThat(entity, containsString("id='" + username + "'"));
        // check roles
        expectedRoles.forEach(role -> assertThat(entity, containsString(":" + role)));
        invalidRoles.forEach(role -> assertThat(entity, not(containsString(":" + role))));

    }

    private Http1ClientResponse callProtected(String uri, String username, String password) {
        // here we call the endpoint
        return securitySetup.get()
                            .uri(uri)
                            .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, username)
                            .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, password)
                            .request();
    }
}
