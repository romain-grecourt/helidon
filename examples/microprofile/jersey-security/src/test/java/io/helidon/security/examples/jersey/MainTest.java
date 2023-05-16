/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.security.examples.jersey;

import java.util.Set;

import io.helidon.microprofile.tests.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.HTTP_AUTHENTICATION_PASSWORD;
import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.HTTP_AUTHENTICATION_USERNAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("SameParameterValue")
@HelidonTest
public class MainTest {

    @Inject
    private WebTarget target;

    @Test
    public void testUnprotected() {
        try (Response response = target.request()
                                       .get()) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), containsString("<ANONYMOUS>"));
        }
    }

    @Test
    public void testProtectedOk() {
        testProtected("/protected", "jack", "password", Set.of("user", "admin"), Set.of());
        testProtected("/protected", "jill", "password", Set.of("user"), Set.of("admin"));
    }

    @Test
    public void testWrongPwd() {
        // here we call the endpoint
        try (Response response = callProtected("/protected", "jack", "somePassword")) {
            assertThat(response.getStatus(), is(401));
        }
    }

    @Test
    public void testDenied() {
        testProtectedDenied("/protected", "john", "password");
    }

    @Test
    public void testOutboundOk() {
        testProtected("/outbound", "jill", "password", Set.of("user"), Set.of("admin"));
    }

    private Response callProtected(String path, String username, String password) {
        // here we call the endpoint
        return target.path(path)
                     .request()
                     .property(HTTP_AUTHENTICATION_USERNAME, username)
                     .property(HTTP_AUTHENTICATION_PASSWORD, password)
                     .get();
    }

    private void testProtectedDenied(String path, String username, String password) {
        try (Response response = callProtected(path, username, password)) {
            assertThat(response.getStatus(), is(403));
        }
    }

    private void testProtected(String path,
                               String username,
                               String password,
                               Set<String> expectedRoles,
                               Set<String> invalidRoles) {

        try (Response response = callProtected(path, username, password)) {
            String entity = response.readEntity(String.class);
            assertThat(response.getStatus(), is(200));
            // check login
            assertThat(entity, containsString("id='" + username + "'"));
            // check roles
            expectedRoles.forEach(role -> assertThat(entity, containsString(":" + role)));
            invalidRoles.forEach(role -> assertThat(entity, not(containsString(":" + role))));
        }
    }
}
