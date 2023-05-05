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

import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbStatementDml;
import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.DbStatementContext;

import com.mongodb.client.MongoDatabase;

/**
 * DML statement for MongoDB.
 */
public class MongoDbStatementDml extends MongoDbStatement<DbStatementDml, Long> implements DbStatementDml {

    private DbStatementType type;
    private MongoStatement stmt;

    MongoDbStatementDml(MongoDatabase db, DbStatementContext statementContext) {
        super(db, statementContext);
        this.type = statementContext.statementType();
    }

    @Override
    public Long execute() {
        stmt = new MongoStatement(type, build());
        switch (stmt.getOperation()) {
            case INSERT -> type = DbStatementType.INSERT;
            case UPDATE -> type = DbStatementType.UPDATE;
            case DELETE -> type = DbStatementType.DELETE;
            default -> throw new IllegalStateException(String.format(
                    "Unexpected value for DML statement: %s",
                    stmt.getOperation()));
        }
        return super.execute();
    }

    @Override
    protected Long doExecute(DbClientServiceContext dbContext) {
        return MongoDbDMLExecutor.executeDml(this, type, stmt);
    }

    @Override
    protected DbStatementType statementType() {
        return type;
    }
}
