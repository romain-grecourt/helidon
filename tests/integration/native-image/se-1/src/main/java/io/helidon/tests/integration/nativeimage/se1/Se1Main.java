/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.nativeimage.se1;

import java.nio.file.Paths;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.config.FileSystemWatcher;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.http.media.jsonb.JsonbSupport;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;
import io.helidon.nima.webserver.staticcontent.StaticContentService;
import io.helidon.nima.webserver.tracing.TracingFeature;
import io.helidon.nima.websocket.webserver.WsRouting;
import io.helidon.security.integration.nima.SecurityFeature;
import io.helidon.tracing.TracerBuilder;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;

/**
 * Main class of this integration test.
 */
public final class Se1Main {
    /**
     * Cannot be instantiated.
     */
    private Se1Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        startServer();
    }

    /**
     * Start the server.
     *
     * @return the created {@link WebServer} instance
     */
    static WebServer startServer() {
        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = buildConfig();

        // Get webserver config from the "server" section of application.yaml
        WebServer server = setup(WebServer.builder(), config)
                .start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.

        System.out.println("WEB server is up! http://localhost:" + server.port() + "/greet");
        return server;
    }

    static WebServer.Builder setup(WebServer.Builder builder, Config config) {
        return builder.routing(routing -> routing(routing, config))
               .addRouting(WsRouting.builder()
                                    .endpoint("/ws/messages", WebSocketEndpoint::new)
                                    .build())
               .config(config.get("server"))
               .addMediaSupport(JsonpSupport.create())
               .addMediaSupport(JsonbSupport.create());
//                                    .printFeatureDetails(true)
    }

    private static Config buildConfig() {
        return Config.builder()
                     .sources(
                             classpath("se-test.yaml").optional(),
                             file("conf/se.yaml")
                                     .changeWatcher(FileSystemWatcher.create())
                                     .optional(),
                             classpath("application.yaml"))
                     .build();
    }

    static void routing(HttpRouting.Builder routing, Config config) {
        GreetService greetService = new GreetService(config);
        ColorService colorService = new ColorService(config);
        MockZipkinService zipkinService = new MockZipkinService(Set.of("helidon-nima-webclient"));
        WebClientService webClientService = new WebClientService(config, zipkinService);
        routing.register("/static/path", StaticContentService.create(Paths.get("web")))
               .register("/static/classpath", StaticContentService.create("web"))
               .register("/static/jar", StaticContentService.create("web-jar"))
               .register("/greet", greetService)
               .register("/color", colorService)
               .register("/wc", webClientService)
               .register("/zipkin", zipkinService)
               .addFeature(SecurityFeature.create(config.get("security")))
               .addFeature(ObserveFeature.create())
               .addFeature(TracingFeature.create(TracerBuilder.create(config.get("tracing")).build()));
    }
}
