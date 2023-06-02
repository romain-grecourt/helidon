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

package io.helidon.examples.micrometer.se;

import io.helidon.config.Config;
import io.helidon.integrations.micrometer.MicrometerFeature;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

/**
 * Simple Hello World rest application.
 */
public final class Main {

    static final String PERSONALIZED_GETS_COUNTER_NAME = "personalizedGets";
    static final String ALL_GETS_TIMER_NAME = "allGets";

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

        WebServer.Builder builder = WebServer.builder()
                                             .config(config.get("server"));
        setup(builder, config);
        WebServer server = builder.start();
        System.out.println("WEB server is up! http://localhost:" + server.port() + "/greet");
    }

    /**
     * Setup the server.
     * @param server server builder
     * @param config config
     */
    static void setup(WebServer.Builder server, Config config) {

        // Get webserver config from the "server" section of application.yaml
        server.routing(r -> routing(r, config));
    }

    /**
     * Setup routing.
     */
    private static void routing(HttpRouting.Builder routing, Config config) {

        MicrometerFeature micrometer = MicrometerFeature.create();
        Counter personalizedGetCounter = micrometer.registry()
                                                   .counter(PERSONALIZED_GETS_COUNTER_NAME);
        Timer getTimer = Timer.builder(ALL_GETS_TIMER_NAME)
                              .publishPercentileHistogram()
                              .register(micrometer.registry());

        GreetService greetService = new GreetService(config, getTimer, personalizedGetCounter);

        routing.addFeature(micrometer)                 // Micrometer support at "/micrometer"
               .register("/greet", greetService);
    }
}
