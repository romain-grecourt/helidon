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
package io.helidon.dbclient.mongodb;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbClientContext;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * MongoDB driver handler.
 */
public class MongoDbClient implements DbClient {
    private final MongoDbClientConfig config;
    private final MongoClient client;
    private final MongoDatabase db;
    private final ConnectionString connectionString;
    private final DbClientContext clientContext;

    /**
     * Creates an instance of MongoDB driver handler.
     *
     * @param builder builder for mongoDB database
     */
    MongoDbClient(MongoDbClientProviderBuilder builder) {
        this.clientContext = DbClientContext.builder()
                                            .dbMapperManager(builder.dbMapperManager())
                                            .mapperManager(builder.mapperManager())
                                            .clientServices(builder.clientServices())
                                            .statements(builder.statements())
                                            .build();

        this.config = builder.dbConfig();
        this.connectionString = new ConnectionString(config.url());
        this.client = initMongoClient();
        this.db = initMongoDatabase();
    }

    /**
     * Creates an instance of MongoDB driver handler with MongoDb client and connection
     * supplied.
     * Used in jUnit tests to mock MongoDB driver internals.
     *
     * @param builder builder for mongoDB database
     * @param client  MongoDB client provided externally
     * @param db      MongoDB database provided externally
     */
    MongoDbClient(MongoDbClientProviderBuilder builder, MongoClient client, MongoDatabase db) {
        this.clientContext = DbClientContext.builder()
                                            .dbMapperManager(builder.dbMapperManager())
                                            .mapperManager(builder.mapperManager())
                                            .clientServices(builder.clientServices())
                                            .statements(builder.statements())
                                            .build();

        this.config = builder.dbConfig();
        this.connectionString = config != null ? new ConnectionString(config.url()) : null;
        this.client = client;
        this.db = db;
    }

    @Override
    public <T> T transaction(Function<DbExecute, T> function) {
        throw new UnsupportedOperationException("Transactions are not yet supported in MongoDB");
    }

    @Override
    public DbExecute execute() {
        return new MongoDbExecute(db, clientContext);
    }

    @Override
    public String dbType() {
        return MongoDbClientProvider.DB_TYPE;
    }

    @Override
    public <C> C unwrap(Class<C> cls) {
        if (MongoClient.class.isAssignableFrom(cls) || MongoDatabase.class.isAssignableFrom(cls)) {
            return cls.cast(client);
        }
        throw new UnsupportedOperationException(String.format(
                "Class %s is not supported for unwrap",
                cls.getName()));
    }

    /**
     * Constructor helper to build MongoDB client from provided configuration.
     */
    private MongoClient initMongoClient() {
        MongoClientSettings.Builder settingsBuilder =
                MongoClientSettings.builder().applyConnectionString(connectionString);

        if ((config.username() != null) || (config.password() != null)) {
            String credDb = (config.credDb() == null) ? connectionString.getDatabase() : config.credDb();

            MongoCredential credentials = MongoCredential.createCredential(
                    config.username(),
                    requireNonNull(credDb),
                    config.password().toCharArray());

            settingsBuilder.credential(credentials);
        }

        return MongoClients.create(settingsBuilder.build());
    }

    private MongoDatabase initMongoDatabase() {
        return client.getDatabase(requireNonNull(connectionString.getDatabase()));
    }
}
