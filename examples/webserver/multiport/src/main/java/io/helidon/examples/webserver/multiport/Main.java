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

package io.helidon.examples.webserver.multiport;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.webserver.ListenerConfiguration;
import io.helidon.nima.webserver.Router;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

/**
 * The application main class.
 */
public final class Main {

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        WebServer.Builder builder = WebServer.builder();
        setup(builder, config);
        WebServer server = builder.start();
        System.out.println("WEB server is up! http://localhost:" + server.port());
    }

    /**
     * Setup the server.
     */
    static void setup(WebServer.Builder server, Config config) {
        // Build server using three ports:
        // default public port, admin port, private port
        server.config(config.get("server"))
              .routing(Main::publicRouting)
              // Add a set of routes on the named socket "admin"
              .socket("admin", Main::adminSocket)
              // Add a set of routes on the named socket "private"
              .socket("private", Main::privateSocket);
    }

    /**
     * Setup private socket.
     */
    static void privateSocket(ListenerConfiguration.Builder socket, Router.RouterBuilder<?> router) {
        router.addRouting(HttpRouting.builder()
                                     .get("/private/hello", (req, res) -> res.send("Private Hello!!")));
    }

    /**
     * Setup public routing.
     */
    static void publicRouting(HttpRouting.Builder routing) {
        routing.get("/hello", (req, res) -> res.send("Public Hello!!"));
    }

    /**
     * Setup admin socket.
     */
    static void adminSocket(ListenerConfiguration.Builder socket, Router.RouterBuilder<?> router) {
        router.addRouting(HttpRouting.builder()
                                     .addFeature(ObserveFeature.create()));
    }
}
