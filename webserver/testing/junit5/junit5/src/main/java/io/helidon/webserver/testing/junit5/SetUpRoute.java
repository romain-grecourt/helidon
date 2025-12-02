/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.webserver.WebServer;

/**
 * Mark a static method to configure the routes.
 * <p>
 * Multiple methods may exist, each configuring a different socket (see {@link #value()}).
 * <p>
 * E.g.
 * <pre>
 * &#064;SetUpRoute
 * static void setUp(HttpRouting.Builder routing) {
 *    routing.get("/test", ((req, res) -> res.send("OK!")));
 * }</pre>
 * <p>
 * When using {@link ServerTest}, the socket listener for the given route can be also be configured.
 * <p>
 * E.g.
 * <pre>
 * &#064;SetUpRoute("tls")
 * static void setUp(HttpRouting.Builder routing, ListenerConfig.Builder socket) {
 *    routing.get("/test-tls", ((req, res) -> res.send("OK TLS!")));
 *    socket.tls(tls -> tls
 *         .privateKey(key -> key.pem(pem -> pem
 *                 .key(Resource.create("mycert.key"))))
 *         .privateKeyCertChain(cert -> cert.pem(pem -> pem
 *                 .certChain(Resource.create("mycert.crt")))));
 * }</pre>
 *
 * @see io.helidon.webserver.WebServerConfig.Builder
 * @see io.helidon.webserver.ListenerConfig.Builder
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SetUpRoute {
    /**
     * Socket name for this router.
     *
     * @return name of the socket
     */
    String value() default WebServer.DEFAULT_SOCKET_NAME;
}
