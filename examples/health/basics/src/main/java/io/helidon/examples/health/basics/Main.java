/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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
package io.helidon.examples.health.basics;

import java.time.Duration;

import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.observe.health.HealthFeature;
import io.helidon.nima.observe.health.HealthObserveProvider;
import io.helidon.nima.webserver.WebServer;

/**
 * Main class of health check integration example.
 */
public final class Main {

    private static long serverStartTime;

    private Main() {
    }

    /**
     * Start the example. Prints endpoints to standard output.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        serverStartTime = System.currentTimeMillis();
        HealthCheck exampleHealthCheck = new HealthCheck() {
            @Override
            public String name() {
                return "exampleHealthCheck";
            }

            @Override
            public HealthCheckType type() {
                return HealthCheckType.READINESS;
            }

            @Override
            public HealthCheckResponse call() {
                return HealthCheckResponse.builder()
                                          .status(HealthCheckResponse.Status.UP)
                                          .detail("time", System.currentTimeMillis())
                                          .build();
            }
        };
        HealthCheck exampleStartCheck = new HealthCheck() {
            @Override
            public String name() {
                return "exampleStartCheck";
            }

            @Override
            public HealthCheckType type() {
                return HealthCheckType.STARTUP;
            }

            @Override
            public HealthCheckResponse call() {
                return HealthCheckResponse.builder()
                                          .status(isStarted())
                                          .detail("time", System.currentTimeMillis())
                                          .build();
            }
        };

        HealthFeature health = HealthFeature.builder()
                                            .addCheck(exampleHealthCheck)
                                            .addCheck(exampleStartCheck)
                                            .build();

        ObserveFeature observe = ObserveFeature.builder()
                                               .addProvider(HealthObserveProvider.create(health))
                                               .build();

        WebServer server = WebServer.builder()
                                    .routing(routing -> routing.addFeature(observe)
                                                               .get("/hello", (req, res) -> res.send("Hello World!")))
                                    .start();

        String endpoint = "http://localhost:" + server.port();
        System.out.println("Hello World started on " + endpoint + "/hello");
        System.out.println("Health checks available on " + endpoint + "/health");
    }

    private static boolean isStarted() {
        return Duration.ofMillis(System.currentTimeMillis() - serverStartTime).getSeconds() >= 8;
    }
}
