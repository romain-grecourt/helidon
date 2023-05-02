/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

package io.helidon.grpc.examples.basics;

import io.helidon.config.Config;
import io.helidon.grpc.examples.common.GreetService;
import io.helidon.grpc.examples.common.StringService;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.grpc.webserver.GrpcRouting;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.webserver.WebServer;

/**
 * A basic example of a Helidon gRPC server.
 */
public class Server {

    private Server() {
    }

    /**
     * The main program entry point.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        // load logging configuration
        LogConfig.configureRuntime();

        WebServer server = WebServer.builder()
                                    .config(config.get("server"))
                                    .routing(routing -> routing.addFeature(ObserveFeature.create()))
                                    .addRouting(createRouting(config))
                                    .start();

        System.out.println("gRPC server is UP! http://localhost:" + server.port());
    }

    private static GrpcRouting createRouting(Config config) {
        GreetService greetService = new GreetService(config);
        // TODO
        // GreetServiceJava greetServiceJava = new GreetServiceJava(config);

        return GrpcRouting.builder()
                          .service(greetService)
//                .service(greetServiceJava)
                          .service(new StringService())
                          .build();
    }
}
