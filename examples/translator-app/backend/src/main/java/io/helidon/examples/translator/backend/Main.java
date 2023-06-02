/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

package io.helidon.examples.translator.backend;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.tracing.TracingFeature;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.TracerBuilder;

/**
 * Translator application backend main class.
 */
public class Main {

    private Main() {
    }

    static void setup(WebServer.Builder builder) {
        Config config = Config.builder()
                              .sources(ConfigSources.environmentVariables())
                              .build();

        Tracer tracer = TracerBuilder.create(config.get("tracing"))
                                     .serviceName("helidon-webserver-translator-backend")
                                     .build();

        builder.port(9080)
               .routing(routing -> routing
                       .addFeature(TracingFeature.create(tracer))
                       .register(new TranslatorBackendService()));
    }

    /**
     * The main method of Translator backend.
     *
     * @param args command-line args, currently ignored.
     */
    public static void main(String[] args) {
        // configure logging in order to not have the standard JVM defaults
        LogConfig.configureRuntime();

        WebServer.Builder builder = WebServer.builder();
        setup(builder);
        WebServer server = builder.start();

        System.out.println("WEB server is up! http://localhost:" + server.port());
    }
}
