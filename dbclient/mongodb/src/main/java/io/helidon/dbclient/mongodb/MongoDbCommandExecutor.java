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

package io.helidon.dbclient.mongodb;

import java.lang.System.Logger.Level;
import java.util.stream.Stream;

import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbMapperManager;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.DbClientContext;
import io.helidon.dbclient.mongodb.MongoDbStatement.MongoStatement;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Executes Mongo specific database command and returns result.
 * Utility class with static methods only.
 */
final class MongoDbCommandExecutor {

    private static final System.Logger LOGGER = System.getLogger(MongoDbCommandExecutor.class.getName());

    private MongoDbCommandExecutor() {
        // cannot be instantiated
    }

    static Stream<DbRow> executeCommand(MongoDbStatement<?, ?> dbStmt) {
        MongoStatement stmt = new MongoStatement(DbStatementType.COMMAND, dbStmt.build());
        if (stmt.getOperation() == MongoDbStatement.MongoOperation.COMMAND) {
            return executeCommandInMongoDB(dbStmt, stmt);
        }
        throw new UnsupportedOperationException(String.format(
                "Operation %s is not supported",
                stmt.getOperation().toString()));
    }

    private static Stream<DbRow> executeCommandInMongoDB(MongoDbStatement<?, ?> dbStmt, MongoStatement stmt) {
        MongoDatabase db = dbStmt.db();
        Document command = stmt.getQuery();
        LOGGER.log(Level.DEBUG, () -> String.format("Command: %s", command.toString()));

        DbClientContext clientContext = dbStmt.clientContext();
        DbMapperManager dbMapperManager = clientContext.dbMapperManager();
        MapperManager mapperManager = clientContext.mapperManager();

        Document doc = db.runCommand(command);
        return Stream.of(MongoDbRow.create(doc, dbMapperManager, mapperManager));
    }
}
