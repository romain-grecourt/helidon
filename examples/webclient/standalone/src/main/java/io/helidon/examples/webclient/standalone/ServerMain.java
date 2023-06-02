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
package io.helidon.examples.webclient.standalone;

import io.helidon.config.Config;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

/**
 * The application main class.
 */
public final class ServerMain {

    /**
     * Cannot be instantiated.
     */
    private ServerMain() {
    }

    /**
     * WebServer starting method.
     *
     * @param args starting arguments
     */
    public static void main(String[] args) {
        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        WebServer.Builder builder = WebServer.builder();
        setup(builder, config);
        WebServer server = builder.start();
        System.out.println("WEB server is up! http://localhost:" + server.port() + "/greet");
    }

    /**
     * Setup the server.
     *
     * @param server server builder
     * @param config config
     */
    static void setup(WebServer.Builder server, Config config) {
        server.config(config.get("server"))
              .routing(r -> routing(r, config));
    }

    /**
     * Setup routing.
     *
     * @param routing routing builder
     * @param config  configuration of this server
     */
    private static void routing(HttpRouting.Builder routing, Config config) {
        routing.addFeature(ObserveFeature.create())
               .register("/greet", new GreetService(config));
    }
}
