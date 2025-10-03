/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.common;

import java.util.Map;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.service.registry.Service;
import io.helidon.tests.integration.dbclient.common.model.Pokemons;
import io.helidon.tests.integration.dbclient.common.model.Types;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;
import io.helidon.webserver.spi.ServerFeature;

/**
 * Factories.
 */
@SuppressWarnings("ALL")
public class TestFactories {

    private TestFactories() {
    }

    /**
     * Create configuration.
     *
     * @param overrides overrides
     * @return Config
     */
    public static Config config(Map<String, String> overrides) {
        return Config.create(
                ConfigSources.create(overrides),
                ConfigSources.classpath("application.yaml"),
                ConfigSources.classpath("db-common.yaml"));
    }

    @Weight(Weighted.DEFAULT_WEIGHT + 20)
    @Service.Singleton
    record DefaultConfig() implements Supplier<Config> {
        @Override
        public Config get() {
            return config(Map.of());
        }
    }

    @Service.Singleton
    @Service.Named("healthNoDetails")
    record ServerHealthNoDetails(Config config, DbClient db) implements Supplier<ServerFeature> {

        @Override
        public ServerFeature get() {
            return ObserveFeature.builder()
                    .observersDiscoverServices(false)
                    .endpoint("noDetails")
                    .name("healthNoDetails")
                    .addObserver(
                            HealthObserver.builder()
                                    .addCheck(DbClientHealthCheck.builder(db)
                                            .config(config.get("db.health-check"))
                                            .name("healthNoDetails")
                                            .build())
                                    .details(false)
                                    .build())
                    .build();
        }
    }

    @Service.Singleton
    @Service.Named("healthDetails")
    record ServerHealthDetails(Config config, DbClient db) implements Supplier<ServerFeature> {

        @Override
        public ServerFeature get() {
            return ObserveFeature.builder()
                    .observersDiscoverServices(false)
                    .endpoint("details")
                    .name("healthDetails")
                    .addObserver(
                            HealthObserver.builder()
                                    .addCheck(DbClientHealthCheck.builder(db)
                                            .config(config.get("db.health-check"))
                                            .name("healthDetails")
                                            .build())
                                    .details(true)
                                    .build())
                    .build();
        }
    }

    @Service.Singleton
    record DefaultDbClient(Config config) implements Supplier<DbClient> {

        @Override
        public DbClient get() {
            Config dbConfig = config.get("db");
            DbClient db = DbClient.create(dbConfig);

            // create schema
            boolean createSchema = dbConfig.get("create-schema").asBoolean().orElse(true);
            if (createSchema) {
                DbExecute exec = db.execute();
                exec.namedDml("create-types");
                exec.namedDml("create-pokemons");
                exec.namedDml("create-poketypes");
                exec.namedDml("create-matches");
                if (db.dbType().equals("jdbc:oracle")) {
                    exec.namedDml("create-matches-seq");
                }
            }

            // initialize data-set
            boolean insertDataSet = dbConfig.get("insert-dataset").asBoolean().orElse(true);
            if (insertDataSet) {
                DbExecute exec = db.execute();
                Types.ALL.forEach(t -> exec.namedInsert("insert-type", t.id(), t.name()));
                Pokemons.ALL.forEach(p -> exec.namedInsert("insert-pokemon", p.id(), p.name()));
                Pokemons.ALL.forEach(p -> p.types().forEach(t -> exec.namedInsert("insert-poketype", p.id(), t.id())));
            }
            return db;
        }
    }
}
