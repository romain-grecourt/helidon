/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.common.tests;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.http.Status;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Observability test.
 */
public interface ObservabilityTest {

    /**
     * Verify health check implementation with default settings.
     */
    void testHealthCheck();

    /**
     * Verify health check implementation with builder and custom name.
     */
    void testHealthCheckWithName();

    /**
     * Verify health check implementation using custom DML named statement.
     */
    void testHealthCheckWithCustomNamedDML();

    /**
     * Verify health check implementation using custom DML statement.
     */
    void testHealthCheckWithCustomDML();

    /**
     * Verify health check implementation using custom query named statement.
     */
    void testHealthCheckWithCustomNamedQuery();

    /**
     * Verify health check implementation using custom query statement.
     */
    void testHealthCheckWithCustomQuery();

    /**
     * HTTP observability test.
     */
    interface HttpObservabilityTest {
        /**
         * Read and check Database Client health status from Helidon Web Server.
         */
        void testHttpHealthNoDetails();

        /**
         * Read and check Database Client health status from Helidon Web Server.
         */
        void testHttpHealthDetails();

        /**
         * Read and check Database Client metrics from Helidon Web Server.
         */
        void testHttpMetrics();

        /**
         * Actual implementation of {@link HttpObservabilityTest}.
         */
        class TestImpl implements HttpObservabilityTest {
            private final Http1Client client;

            /**
             * Create a new instance.
             *
             * @param client client
             */
            public TestImpl(Http1Client client) {
                this.client = client;
            }

            @Override
            public void testHttpHealthNoDetails() {
                // Call select-pokemons
                try (var res = client.get("/pokemon").request()) {
                    assertThat(res.status().code(), is(200));
                }

                ClientResponseTyped<String> response = client.get("/noDetails/health").request(String.class);
                assertThat(response.status(), equalTo(Status.NO_CONTENT_204));
            }

            @Override
            public void testHttpHealthDetails() {
                // Call select-pokemons
                try (var res = client.get("/pokemon").request()) {
                    assertThat(res.status().code(), is(200));
                }

                // Read and process health check response
                ClientResponseTyped<JsonObject> response = client.get("/details/health").request(JsonObject.class);
                assertThat(response.status(), equalTo(Status.OK_200));

                JsonArray checks = response.entity().asJsonObject().getJsonArray("checks");
                assertThat(checks.size(), greaterThan(0));
                for (JsonValue check : checks) {
                    String status = check.asJsonObject().getString("status");
                    assertThat(status, equalTo("UP"));
                }
            }

            @Override
            public void testHttpMetrics() {
                // Read and process metrics response
                JsonObject application = client.get("/observe/metrics/application")
                        .accept(MediaTypes.APPLICATION_JSON)
                        .requestEntity(JsonObject.class);

                int origSelectCount = application.getInt("db.counter.select-pokemons", 0);
                int origInsertCount = application.getInt("db.counter.insert-pokemon", 0);
                JsonObject insertTimer = application.getJsonObject("db.timer.insert-pokemon");
                int origInsertTimerCount = insertTimer == null ? 0 : insertTimer.getInt("count", 0);

                // Call select-pokemons
                try (var res = client.get("/pokemon").request()) {
                    assertThat(res.status().code(), is(200));
                }

                // Call insert-pokemon
                try (var res = client.post("/pokemon")
                        .contentType(MediaTypes.APPLICATION_JSON)
                        .submit("""
                            {"id": 401, "name": "Lickitung"}
                            """)) {
                    assertThat(res.status().code(), is(201));
                }

                // Read and process metrics response
                application = client.get("/observe/metrics/application")
                        .accept(MediaTypes.APPLICATION_JSON)
                        .requestEntity(JsonObject.class);

                int actualSelectCount = application.getInt("db.counter.select-pokemons", 0);
                assertThat(actualSelectCount, is(origSelectCount + 1));

                int actualInsertCount = application.getInt("db.counter.insert-pokemon", 0);
                assertThat(actualInsertCount, equalTo(origInsertCount + 1));
                assertThat(application.containsKey("db.timer.insert-pokemon"), equalTo(true));

                insertTimer = application.getJsonObject("db.timer.insert-pokemon");
                assertThat(insertTimer, is(not(nullValue())));
                assertThat(insertTimer.containsKey("count"), equalTo(true));
                assertThat(insertTimer.containsKey("mean"), equalTo(true));
                assertThat(insertTimer.containsKey("max"), equalTo(true));

                int actualInsertTimerCount = insertTimer.getInt("count");
                assertThat(actualInsertTimerCount, equalTo(origInsertTimerCount + 1));
            }
        }
    }

    /**
     * Actual implementation of {@link ObservabilityTest}.
     */
    final class TestImpl extends AbstractTest implements ObservabilityTest {

        @Override
        public void testHealthCheck() {
            HealthCheck check = DbClientHealthCheck.create(db, config.get("db.health-check"));
            HealthCheckResponse response = check.call();
            HealthCheckResponse.Status state = response.status();
            assertThat("Health check failed, response: " + response.details(), state, equalTo(HealthCheckResponse.Status.UP));
        }

        @Override
        public void testHealthCheckWithName() {
            String hcName = "TestHC";
            HealthCheck check = DbClientHealthCheck.builder(db).config(config.get("db.health-check")).name(hcName).build();
            HealthCheckResponse response = check.call();
            String name = check.name();
            HealthCheckResponse.Status state = response.status();
            assertThat(name, equalTo(hcName));
            assertThat(state, equalTo(HealthCheckResponse.Status.UP));
        }

        @Override
        public void testHealthCheckWithCustomNamedDML() {
            HealthCheck check = DbClientHealthCheck.builder(db).dml().statementName("ping-dml").build();
            HealthCheckResponse response = check.call();
            HealthCheckResponse.Status state = response.status();
            assertThat("Health check failed, response: " + response.details(), state, equalTo(HealthCheckResponse.Status.UP));
        }

        @Override
        public void testHealthCheckWithCustomDML() {
            String statement = statements.get("ping-dml");
            assertThat("Missing ping-dml statement in database configuration!", is(not(nullValue())));
            HealthCheck check = DbClientHealthCheck.builder(db).dml().statement(statement).build();
            HealthCheckResponse response = check.call();
            HealthCheckResponse.Status state = response.status();
            assertThat("Health check failed, response: " + response.details(), state, equalTo(HealthCheckResponse.Status.UP));
        }

        @Override
        public void testHealthCheckWithCustomNamedQuery() {
            HealthCheck check = DbClientHealthCheck.builder(db).query().statementName("ping").build();
            HealthCheckResponse response = check.call();
            HealthCheckResponse.Status state = response.status();
            assertThat("Health check failed, response: " + response.details(), state, equalTo(HealthCheckResponse.Status.UP));
        }

        @Override
        public void testHealthCheckWithCustomQuery() {
            String statement = statements.get("ping");
            assertThat("Missing ping-query statement String in database configuration!", statement, is(not(nullValue())));
            HealthCheck check = DbClientHealthCheck.builder(db).query().statement(statement).build();
            HealthCheckResponse response = check.call();
            HealthCheckResponse.Status state = response.status();
            assertThat("Health check failed, response: " + response.details(), state, equalTo(HealthCheckResponse.Status.UP));
        }
    }
}
