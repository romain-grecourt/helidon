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

import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.mongodb.MongoDbStatement.MongoStatement;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

/**
 * Executes Mongo specific DML statement and returns result.
 * Utility class with static methods only.
 */
final class MongoDbDMLExecutor {

    private static final System.Logger LOGGER = System.getLogger(MongoDbDMLExecutor.class.getName());

    private MongoDbDMLExecutor() {
        throw new UnsupportedOperationException("Utility class MongoDbDMLExecutor instances are not allowed!");
    }

    static Long executeDml(MongoDbStatement<?, ?> dbStmt, DbStatementType type, MongoStatement stmt) {
        try {
            long result = switch (type) {
                case INSERT -> executeInsert(dbStmt, stmt);
                case UPDATE -> executeUpdate(dbStmt, stmt);
                case DELETE -> executeDelete(dbStmt, stmt);
                default -> throw new UnsupportedOperationException(String.format(
                        "Statement operation not yet supported: %s",
                        type.name()));
            };
            if (dbStmt.txManager() != null) {
                dbStmt.txManager().stmtFinished(dbStmt);
            }
            LOGGER.log(Level.DEBUG, () -> String.format(
                    "%s DML %s execution succeeded",
                    type.name(),
                    dbStmt.statementName()));
            return result;
        } catch (UnsupportedOperationException ex) {
            throw ex;
        } catch (Throwable throwable) {
            if (dbStmt.txManager() != null) {
                dbStmt.txManager().stmtFailed(dbStmt);
            }
            LOGGER.log(Level.DEBUG, () -> String.format(
                    "%s DML %s execution failed", type.name(), dbStmt.statementName()));
            throw throwable;
        }
    }

    private static Long executeInsert(MongoDbStatement<?, ?> dbStmt, MongoStatement stmt) {
        MongoCollection<Document> mc = dbStmt.db().getCollection(stmt.getCollection());
        if (dbStmt.noTx()) {
            mc.insertOne(stmt.getValue());
        } else {
            mc.insertOne(dbStmt.txManager().tx(), stmt.getValue());
        }
        return 1L;
    }

    private static Long executeUpdate(MongoDbStatement<?, ?> dbStmt, MongoStatement stmt) {
        MongoCollection<Document> mc = dbStmt.db().getCollection(stmt.getCollection());
        Document query = stmt.getQuery();

        UpdateResult updateResult = dbStmt.noTx()
                ? mc.updateMany(query, stmt.getValue())
                : mc.updateMany(dbStmt.txManager().tx(), query, stmt.getValue());
        return updateResult.getModifiedCount();

    }

    private static Long executeDelete(MongoDbStatement<?, ?> dbStmt, MongoStatement stmt) {
        MongoCollection<Document> mc = dbStmt.db().getCollection(stmt.getCollection());
        Document query = stmt.getQuery();
        DeleteResult deleteResult = dbStmt.noTx()
                ? mc.deleteMany(query)
                : mc.deleteMany(dbStmt.txManager().tx(), query);
        return deleteResult.getDeletedCount();
    }
}
