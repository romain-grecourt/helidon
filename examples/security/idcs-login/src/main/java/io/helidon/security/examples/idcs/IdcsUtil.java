/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

package io.helidon.security.examples.idcs;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

/**
 * IDCS login example utilities.
 */
public class IdcsUtil {
    // do not change this constant, unless you modify configuration
    // of IDCS application redirect URI
    static final int PORT = 7987;

    private IdcsUtil() {
    }

    static WebServer startIt(Consumer<HttpRouting.Builder> routing) {
        return WebServer.builder()
                        .routing(routing)
                        .port(PORT)
                        .host("localhost")
                        .build();
    }

    static WebServer start(WebServer webServer) {
        long beforeStart = System.nanoTime();
        webServer.start();
        long time = System.nanoTime() - beforeStart;

        System.out.printf("Server started in %d ms%n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS));
        System.out.printf("Started server on localhost:%d%n", webServer.port());
        System.out.printf("You can access this example at http://localhost:%d/rest/profile%n", webServer.port());
        System.out.println();
        System.out.println();
        System.out.println("Check application.yaml in case you are behind a proxy to configure it");
        return webServer;
    }
}
