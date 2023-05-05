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

import com.mongodb.client.MongoDatabase;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbStatementDml;
import io.helidon.dbclient.DbStatementGet;
import io.helidon.dbclient.DbStatementQuery;
import io.helidon.dbclient.AbstractDbExecute;
import io.helidon.dbclient.DbClientContext;
import io.helidon.dbclient.DbStatementContext;

import static io.helidon.dbclient.DbStatementType.DELETE;
import static io.helidon.dbclient.DbStatementType.DML;
import static io.helidon.dbclient.DbStatementType.GET;
import static io.helidon.dbclient.DbStatementType.INSERT;
import static io.helidon.dbclient.DbStatementType.QUERY;
import static io.helidon.dbclient.DbStatementType.UPDATE;

/**
 * Execute implementation for MongoDB.
 */
public class MongoDbExecute extends AbstractDbExecute implements DbExecute {

    private final MongoDatabase db;
    private final DbClientContext ctx;

    MongoDbExecute(MongoDatabase db, DbClientContext clientContext) {
        super(clientContext.statements());
        this.db = db;
        this.ctx = clientContext;
    }

    @Override
    public DbStatementQuery createNamedQuery(String name, String stmt) {
        return new MongoDbStatementQuery(db, DbStatementContext.create(ctx, QUERY, name, stmt));
    }

    @Override
    public DbStatementGet createNamedGet(String name, String stmt) {
        return new MongoDbStatementGet(db, DbStatementContext.create(ctx, GET, name, stmt));
    }

    @Override
    public DbStatementDml createNamedDmlStatement(String name, String stmt) {
        return new MongoDbStatementDml(db, DbStatementContext.create(ctx, DML, name, stmt));
    }

    @Override
    public DbStatementDml createNamedInsert(String name, String stmt) {
        return new MongoDbStatementDml(db, DbStatementContext.create(ctx, INSERT, name, stmt));
    }

    @Override
    public DbStatementDml createNamedUpdate(String name, String stmt) {
        return new MongoDbStatementDml(db, DbStatementContext.create(ctx, UPDATE, name, stmt));
    }

    @Override
    public DbStatementDml createNamedDelete(String name, String stmt) {
        return new MongoDbStatementDml(db, DbStatementContext.create(ctx, DELETE, name, stmt));
    }

    @Override
    public <C> C unwrap(Class<C> cls) {
        if (MongoDatabase.class.isAssignableFrom(cls)) {
            return cls.cast(db);
        }
        throw new UnsupportedOperationException(String.format(
                "Class %s is not supported for unwrap",
                cls.getName()));
    }

    @Override
    public void close() {
    }
}
