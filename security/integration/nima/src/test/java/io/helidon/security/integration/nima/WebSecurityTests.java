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

import java.util.Set;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.WebClient;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.security.AuditEvent;
import io.helidon.security.Security;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.security.providers.httpauth.HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD;
import static io.helidon.security.providers.httpauth.HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A set of tests that are used both by configuration based
 * and programmatic tests.
 */
@SuppressWarnings("resource")
abstract class WebSecurityTests {
    static final String AUDIT_MESSAGE_FORMAT = "Unit test message format";
    static UnitTestAuditProvider myAuditProvider;
    static WebServer server;
    private static Http1Client securitySetup;
    private static Http1Client webClient;

    @BeforeAll
    static void buildClients() {
        Security security = Security.builder()
                                    .addProvider(HttpBasicAuthProvider.builder().build())
                                    .build();

        securitySetup = WebClient.builder()
                                 .service(ClientSecurity.create(security))
                                 .build();

        webClient = WebClient.builder().build();
    }

    @AfterAll
    static void stopIt() {
        server.stop();
    }

    abstract String serverBaseUri();

    @Test
    void basicTestJohn() {
        String username = "john";
        String password = "password";

        testProtected(serverBaseUri() + "/noRoles", username, password, Set.of(), Set.of());
        // this user has no roles, all requests should fail except for public
        testForbidden(serverBaseUri() + "/user", username, password);
        testForbidden(serverBaseUri() + "/admin", username, password);
        testForbidden(serverBaseUri() + "/deny", username, password);
    }

    @Test
    void basicTestJack() {
        String username = "jack";
        String password = "jackIsGreat";

        testProtected(serverBaseUri() + "/noRoles",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testProtected(serverBaseUri() + "/user",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testProtected(serverBaseUri() + "/admin",
                username,
                password,
                Set.of("user", "admin"),
                Set.of());
        testForbidden(serverBaseUri() + "/deny", username, password);
    }

    @Test
    void basicTestJill() {
        String username = "jill";
        String password = "password";

        testProtected(serverBaseUri() + "/noRoles",
                username,
                password,
                Set.of("user"),
                Set.of("admin"));
        testProtected(serverBaseUri() + "/user",
                username,
                password,
                Set.of("user"),
                Set.of("admin"));
        testForbidden(serverBaseUri() + "/admin", username, password);
        testForbidden(serverBaseUri() + "/deny", username, password);
    }

    @Test
    void basicTest401() {
        Http1ClientResponse response = webClient.get()
                                                .uri(serverBaseUri() + "/noRoles")
                                                .request();
        assertThat(response.status(), is(Http.Status.UNAUTHORIZED_401));

        String header = response.headers()
                                .first(Http.Header.WWW_AUTHENTICATE)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Header " + Http.Header.WWW_AUTHENTICATE + " is" + " not present in response!"));

        assertThat(header.toLowerCase(), is("basic realm=\"mic\""));

        response = callProtected(serverBaseUri() + "/noRoles", "invalidUser", "invalidPassword");
        assertThat(response.status(), is(Http.Status.UNAUTHORIZED_401));

        header = response.headers()
                         .first(Http.Header.WWW_AUTHENTICATE)
                         .orElseThrow(() -> new IllegalStateException(
                                 "Header " + Http.Header.WWW_AUTHENTICATE + " is" + " not present in response!"));
        assertThat(header.toLowerCase(), is("basic realm=\"mic\""));
    }

    @Test
    void testCustomizedAudit() {
        Http1ClientResponse response = webClient.get()
                                                .uri(serverBaseUri() + "/auditOnly")
                                                .request();
        assertThat(response.status(), is(Http.Status.OK_200));

        // audit
        AuditEvent auditEvent = myAuditProvider.getAuditEvent();
        assertThat(auditEvent, notNullValue());
        assertThat(auditEvent.messageFormat(), is(AUDIT_MESSAGE_FORMAT));
        assertThat(auditEvent.severity(), is(AuditEvent.AuditSeverity.SUCCESS));
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

        String entity = response.entity()
                                .as(String.class);

        // check login
        assertThat(entity, containsString("id='" + username + "'"));
        // check roles
        expectedRoles.forEach(role -> assertThat(entity, containsString(":" + role)));
        invalidRoles.forEach(role -> assertThat(entity, not(containsString(":" + role))));

    }

    private Http1ClientResponse callProtected(String uri, String username, String password) {
        return securitySetup.get()
                            .uri(uri)
                            .property(EP_PROPERTY_OUTBOUND_USER, username)
                            .property(EP_PROPERTY_OUTBOUND_PASSWORD, password)
                            .request();
    }

}
