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

package io.helidon.examples.integrations.neo4j.se;

import io.helidon.config.Config;
import io.helidon.examples.integrations.neo4j.se.domain.MovieRepository;
import io.helidon.health.HealthCheckType;
import io.helidon.integrations.neo4j.Neo4j;
import io.helidon.integrations.neo4j.health.Neo4jHealthCheck;
import io.helidon.integrations.neo4j.metrics.Neo4jMetricsSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.http.media.jsonb.JsonbSupport;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.observe.ObserveFeature;
import io.helidon.nima.observe.health.HealthFeature;
import io.helidon.nima.observe.health.HealthObserveProvider;
import io.helidon.nima.observe.metrics.MetricsFeature;
import io.helidon.nima.observe.metrics.MetricsObserveProvider;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import org.neo4j.driver.Driver;

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

        WebServer server = WebServer.builder()
                                    .config(config.get("server"))
                                    .addMediaSupport(JsonpSupport.create())
                                    .addMediaSupport(JsonbSupport.create())
                                    .build()
                                    .start();

        System.out.println(
                "WEB server is up! http://localhost:" + server.port() + "/api/movies");
    }

    /**
     * Updates HTTP Routing.
     */
    static void routing(HttpRouting.Builder routing, Config config) {

        Neo4j neo4j = Neo4j.create(config.get("neo4j"));

        // registers all metrics
        Neo4jMetricsSupport.builder()
                           .driver(neo4j.driver())
                           .build()
                           .initialize();

        Neo4jHealthCheck healthCheck = Neo4jHealthCheck.create(neo4j.driver());

        Driver neo4jDriver = neo4j.driver();

        MovieService movieService = new MovieService(new MovieRepository(neo4jDriver));

        routing.addFeature(ObserveFeature.builder()
                                         .useSystemServices(false)
                                         .addProvider(HealthObserveProvider.create(
                                                 HealthFeature.builder()
                                                              .useSystemServices(true)
                                                              .addCheck(healthCheck, HealthCheckType.READINESS)
                                                              .build()))
                                         .addProvider(MetricsObserveProvider.create(
                                                 MetricsFeature.builder()
                                                               .registryFactory(null) // TODO
                                                               .build())))
               .register(movieService)
               .build();
    }
}
