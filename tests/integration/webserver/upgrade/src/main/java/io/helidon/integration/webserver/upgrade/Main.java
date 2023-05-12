/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.integration.webserver.upgrade;

import io.helidon.common.configurable.Resource;
import io.helidon.common.http.PathMatchers;
import io.helidon.logging.common.LogConfig;
import io.helidon.microprofile.tyrus.TyrusRouting;
import io.helidon.nima.http2.webserver.Http2Route;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http1.Http1Route;

import jakarta.websocket.server.ServerEndpointConfig;

import static io.helidon.common.http.Http.Method.GET;
import static io.helidon.common.http.Http.Method.POST;
import static io.helidon.common.http.Http.Method.PUT;


public class Main {

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        startServer(true);
    }

    public static WebServer startServer(boolean ssl) {
        return WebServer.builder()
                        .defaultSocket(s -> {
                            s.host("localhost");
                            if (ssl) {
                                s.tls(tlsBuilder -> tlsBuilder
                                        .privateKey(keyConfigBuilder -> keyConfigBuilder
                                                .keystorePassphrase("password")
                                                .keystore(Resource.create("server.p12"))));
                            }
                        })
                        .routing(r -> r
                                .get("/", (req, res) -> res.send("HTTP Version " + req.prologue().protocolVersion() + "\n"))
                                .route(Http1Route.route(GET, "/versionspecific", (req, res) -> res.send("HTTP/1.1 route\n")))
                                .route(Http2Route.route(GET, "/versionspecific", (req, res) -> res.send("HTTP/2.0 route\n")))
                                .route(Http1Route.route(GET, "/versionspecific1", (req, res) -> res.send("HTTP/1.1 route\n")))
                                .route(Http2Route.route(GET, "/versionspecific2", (req, res) -> res.send("HTTP/2.0 route\n")))
                                .route(Http1Route.route(
                                        PathMatchers.create("/multi*"),
                                        (req, res) -> res.send("HTTP/1.1 route " + req.prologue().method() + "\n"),
                                        GET, POST, PUT))
                                .route(Http2Route.route(
                                        PathMatchers.create("/multi*"),
                                        (req, res) -> res.send("HTTP/2.0 route " + req.prologue().method() + "\n"),
                                        GET, POST, PUT)))
                        .addRouting(TyrusRouting.builder()
                                                .endpoint("/ws-conf", ServerEndpointConfig.Builder.create(ConfiguredEndpoint.class, "/echo")
                                                                                                  .build())
                                                .endpoint("/ws-annotated", AnnotatedEndpoint.class))
                .start(); // also /echo
    }
}
