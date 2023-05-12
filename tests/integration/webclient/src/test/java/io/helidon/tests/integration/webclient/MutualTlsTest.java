/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.webclient;

import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.nima.common.tls.Tls;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.Router;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test is testing ability to set mutual TLS on WebClient and WebServer.
 */
@ServerTest
public class MutualTlsTest {

    private static final Config CONFIG = Config.just(() -> ConfigSources.classpath("application-test.yaml").build());

    private static WebServer server;

    public MutualTlsTest(WebServer server) {
        this.server = server;
    }

    @SetUpServer
    public static void setUpServer(WebServer.Builder builder) {
        builder.config(CONFIG.get("server"))
               .routing(MutualTlsTest::plainRouting)
               .router("secured", MutualTlsTest::mTlsRouting)
               .router("invalid-server-cert", MutualTlsTest::mTlsRouting)
               .router("client-no-ca", MutualTlsTest::mTlsRouting)
               .router("optional", MutualTlsTest::mTlsRouting);
    }

    @Test
    public void testAccessSuccessful() {
        Http1Client webClient = createWebClient(CONFIG.get("success"));
        String result = webClient.get()
                                 .uri("http://localhost:" + server.port())
                                 .request(String.class);

        assertThat(result, is("Hello world unsecured!"));
        assertThat(exec(webClient, "https", server.port("secured")), is("Hello Helidon-client!"));
    }

    @Test
    public void testNoClientCert() {
        Http1Client client = createWebClient(CONFIG.get("no-client-cert"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> exec(client, "https", server.port("secured")));
        assertThat(ex.getMessage(), containsString("Received fatal alert: bad_certificate"));
    }

    @Test
    public void testOptionalAuthentication() {
        Http1Client client = createWebClient(CONFIG.get("no-client-cert"));

        assertThat(exec(client, "https", server.port("optional")), is("Hello Unknown CN!"));

        client = createWebClient(CONFIG.get("success"));
        assertThat(exec(client, "https", server.port("optional")), is("Hello Helidon-client!"));
    }

    @Test
    public void testServerCertInvalidCn() {
        int port = server.port("invalid-server-cert");
        Http1Client clientOne = createWebClient(CONFIG.get("server-cert-invalid-cn"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> exec(clientOne, "https", port));
        assertThat(ex.getMessage(), is("No name matching localhost found"));

        Http1Client webClientTwo = createWebClient(CONFIG.get("client-disable-hostname-verification"));
        assertThat(exec(webClientTwo, "https", port), is("Hello Helidon-client!"));
    }

    @Test
    public void testClientNoCa() {
        int port = server.port("client-no-ca");
        Http1Client clientOne = createWebClient(CONFIG.get("client-no-ca"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> exec(clientOne, "https", port));
        assertThat(ex.getMessage(), endsWith("unable to find valid certification path to requested target"));

        Http1Client webClientTwo = createWebClient(CONFIG.get("client-trust-all"));
        assertThat(exec(webClientTwo, "https", port), is("Hello Helidon-client!"));
    }

    @Test
    public void testClientAndServerUpdateTls() {
        int portSecured = server.port("secured");
        int portDefault = server.port();
        Http1Client clientFirst = createWebClient(CONFIG.get("success"));
        Http1Client clientSecond = createWebClient(CONFIG.get("client-second-valid"));

        assertThat(exec(clientFirst, "https", portSecured), is("Hello Helidon-client!"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> exec(clientSecond, "https", portSecured));
        assertThat(ex.getMessage(), endsWith("unable to find valid certification path to requested target"));
        String response = clientFirst.get().uri("http://localhost:" + portDefault + "/reload").request(String.class);
        assertThat(response, is("SslContext reloaded. Affected named socket: secured"));

        assertThat(exec(clientSecond, "https", portSecured), is("Hello Oracle-client!"));

        ex = assertThrows(RuntimeException.class, () -> exec(clientFirst, "https", portSecured));
        assertThat(ex.getMessage(), endsWith("unable to find valid certification path to requested target"));

        response = clientFirst.get().uri("http://localhost:" + portDefault + "/reload").request(String.class);
        assertThat(response, is("SslContext reloaded. Affected named socket: secured"));

        assertThat(exec(clientFirst, "https", portSecured), is("Hello Helidon-client!"));
        ex = assertThrows(RuntimeException.class, () -> exec(clientSecond, "https", portSecured));
        assertThat(ex.getMessage(), endsWith("unable to find valid certification path to requested target"));
    }

    private Http1Client createWebClient(Config config) {
        return Http1Client.builder()
                          .config(config)
//                .keepAlive(false)
                          .build();
    }

    private String exec(Http1Client webClient, String schema, int port) {
        return webClient.get()
                        .uri(schema + "://localhost:" + port)
                        .request(String.class);
    }

    private static void plainRouting(HttpRouting.Builder routing) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        routing.get("/", (req, res) -> res.send("Hello world unsecured!"))
               .get("/reload",
                       (req, res) -> {
                           String configName = atomicBoolean.getAndSet(!atomicBoolean.get())
                                   ? "server-second-valid.tls"
                                   : "server.sockets.0.ssl";
                           // ugh ?!
                           server.reloadTls("secured", Tls.create(CONFIG.get(configName)));
                           res.send("SslContext reloaded. Affected named socket: secured");
                       })
               .build();
    }

    private static void mTlsRouting(Router.Builder router) {
        router.addRouting(
                HttpRouting.builder()
                           .get("/", (req, res) -> {
                               String cn = req.headers().first(Http.Header.X_HELIDON_CN).orElse("Unknown CN");
                               res.send("Hello " + cn + "!");
                           }));
    }

}
