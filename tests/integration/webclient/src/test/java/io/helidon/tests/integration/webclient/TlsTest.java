/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.util.concurrent.CompletionException;

import io.helidon.common.configurable.Resource;

import io.helidon.nima.common.tls.Tls;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.WebServer;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WebClient TLS connection.
 */
@ServerTest
public class TlsTest {

    private static WebServer server;
    private static Http1Client webClient;

    public TlsTest(WebServer server) {
        this.server = server;
        webClient = Http1Client.builder()
                               .baseUri("https://localhost:" + server.port())
                               .tls(Tls.builder()
                                       .trustAll(true)
                                       .build())
                               .build();
    }

    @SetUpServer
    public static void setUp(WebServer.Builder builder) {
        builder.routing(routing -> routing.any((req, res) -> res.send("It works!")))
               .tls(tlsBuilder -> tlsBuilder
                       .privateKey(keyConfigBuilder -> keyConfigBuilder
                               .keystorePassphrase("password")
                               .keystore(Resource.create("server.p12"))));
    }

    @Test
    public void testConnectionOnHttpsWithHttp() {
        RuntimeException ex = assertThrows(CompletionException.class, () ->
                webClient.get()
                         .uri("http://localhost:" + server.port())
                         .request(String.class));
        assertThat(ex, instanceOf(RuntimeException.class));
        assertThat(ex.getMessage(), is("Connection reset by the host"));
    }

    @Test
    public void testConnectionOnHttpsWithHttpWithoutKeepAlive() {
        RuntimeException ex = assertThrows(CompletionException.class,
                () -> webClient.get()
                               //.keepAlive(false)
                               .uri("http://localhost:" + server.port())
                               .request(String.class));
        assertThat(ex, instanceOf(RuntimeException.class));
        assertThat(ex.getMessage(), is("Connection reset by the host"));
    }

}
