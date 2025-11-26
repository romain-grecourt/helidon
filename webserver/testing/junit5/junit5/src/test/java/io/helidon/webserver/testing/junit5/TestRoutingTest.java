/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

package io.helidon.webserver.testing.junit5;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.spi.ServerFeature;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RoutingTest
@SuppressWarnings("ClassCanBeRecord")
class TestRoutingTest {
    private static final String ENTITY = "some nice entity";
    private static final String ADMIN_ENTITY = "admin entity";

    private final DirectClient client;

    TestRoutingTest(DirectClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder router) {
        router.get("/get", (req, res) -> res.send(ENTITY))
                .post("/post", (req, res) -> {
                    var requestEntity = req.content().as(String.class);
                    if (ENTITY.equals(requestEntity)) {
                        res.status(Status.CREATED_201);
                    } else {
                        res.status(Status.INTERNAL_SERVER_ERROR_500);
                    }
                    res.send();
                })
                .get("/name", (req, res) -> {
                    var name = req.remotePeer().tlsPrincipal().map(Principal::getName).orElse(null);
                    if (name == null) {
                        res.status(Status.BAD_REQUEST_400).send("Expected client principal");
                    } else {
                        res.send(name);
                    }
                })
                .get("/certs", (req, res) -> {
                    var certs = req.remotePeer().tlsCertificates().orElse(null);
                    if (certs == null) {
                        res.status(Status.BAD_REQUEST_400).send("Expected client certificate");
                    } else {
                        var certDefs = new ArrayList<String>();
                        for (var cert : certs) {
                            if (cert instanceof X509Certificate x509) {
                                certDefs.add("X.509:" + x509.getSubjectX500Principal().getName());
                            } else {
                                certDefs.add(cert.getType());
                            }
                        }
                        res.send(String.join("|", certDefs));
                    }
                });
    }

    @SetUpRoute("admin")
    static void adminRouting(HttpRouting.Builder router) {
        router.get("/get", (req, res) -> res.send(ADMIN_ENTITY));
    }

    @SetUpFeatures
    static List<ServerFeature> features() {
        return List.of(new TestFeature());
    }

    @Test
    void testFeatureSetUp() {
        assertThat(TestFeature.isSetUp(), is(true));
    }

    @Test
    void testGet() {
        try (var response = client.get("/get")
                .request()) {

            String entity = response.as(String.class);
            assertThat(entity, is(ENTITY));
        }
    }

    @Test
    void testInjectGet(@Socket("admin") DirectClient client) {
        try (var response = client.get("/get")
                .request()) {
            var entity = response.as(String.class);
            assertThat(entity, is(ADMIN_ENTITY));
        }
    }

    @Test
    void testInjectDefaultGet(@Socket("@default") DirectClient client) {
        // Test Explicit @default route
        try (var response = client.get("/get").request()) {
            var entity = response.as(String.class);
            assertThat(entity, is(ENTITY));
        }
    }

    @Test
    void testPost() {
        try (var response = client.method(Method.POST)
                .uri("/post")
                .submit(ENTITY)) {
            assertThat(response.status(), is(Status.CREATED_201));
        }
    }

    @Test
    void testMutualTlsPrincipal() {
        var principal = "Custom principal name";
        client.clientTlsPrincipal(new TestPrincipal(principal));
        try (var response = client.method(Method.GET)
                .uri("/name")
                .request()) {

            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.as(String.class), is(principal));
        }
    }

    private record TestPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }

    private static class TestFeature implements ServerFeature {
        private static final AtomicBoolean SET_UP = new AtomicBoolean();

        @Override
        public void setup(ServerFeatureContext featureContext) {
            SET_UP.set(true);
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public String type() {
            return "test";
        }

        static boolean isSetUp() {
            return SET_UP.get();
        }
    }
}
