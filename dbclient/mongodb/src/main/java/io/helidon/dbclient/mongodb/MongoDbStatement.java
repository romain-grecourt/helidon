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

import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoDatabase;
import io.helidon.dbclient.DbStatement;
import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.AbstractStatement;
import io.helidon.dbclient.DbStatementContext;

import jakarta.json.Json;
import org.bson.Document;

/**
 * Common MongoDB statement builder.
 *
 * @param <S> MongoDB statement type
 * @param <R> Statement execution result type
 */
abstract class MongoDbStatement<S extends DbStatement<S, R>, R> extends AbstractStatement<S, R> {

    /**
     * Empty JSON object.
     */
    static final Document EMPTY = Document.parse(Json.createObjectBuilder().build().toString());

    /**
     * Operation JSON parameter name.
     */
    protected static final String JSON_OPERATION = "operation";
    /**
     * Collection JSON parameter name.
     */
    protected static final String JSON_COLLECTION = "collection";
    /**
     * Query JSON parameter name.
     */
    protected static final String JSON_QUERY = "query";
    /**
     * Value JSON parameter name.
     */
    protected static final String JSON_VALUE = "value";
    /**
     * Projection JSON parameter name: Defines projection to restrict returned fields.
     */
    protected static final String JSON_PROJECTION = "projection";

    /** MongoDB database. */
    private final MongoDatabase db;

    /**
     * Creates an instance of MongoDB statement builder.
     *
     * @param db                 mongo database handler
     * @param statementContext   configuration of statement
     */
    MongoDbStatement(MongoDatabase db, DbStatementContext statementContext) {
        super(statementContext);
        this.db = db;
    }

    String build() {
        return switch (paramType()) {
            // Statement shall not contain any parameters, no conversion is needed.
            case UNKNOWN -> statement();
            case INDEXED -> StatementParsers.indexedParser(statement(), indexedParams()).convert();
            // Replace parameter identifiers with values from name to value map
            case NAMED -> StatementParsers.namedParser(statement(), namedParams()).convert();
        };
    }

    /**
     * Statement name.
     *
     * @return name of this statement (never null, may be generated)
     */
    @Override
    public String statementName() {
        return super.statementName();
    }

    MongoDatabase db() {
        return db;
    }

    @Override
    protected String dbType() {
        return MongoDbClientProvider.DB_TYPE;
    }

    /**
     * Mongo operation enumeration.
     */
    enum MongoOperation {
        QUERY("query", "find", "select"),
        INSERT("insert"),
        UPDATE("update"),
        DELETE("delete"),
        // Database command not related to a specific collection
        // Only executable using generic statement
        COMMAND("command");

        private static final Map<String, MongoOperation> NAME_TO_OPERATION = new HashMap<>();

        static {
            for (MongoOperation value : MongoOperation.values()) {
                for (String name : value.names) {
                    NAME_TO_OPERATION.put(name.toLowerCase(), value);
                }
            }
        }

        static MongoOperation operationByName(String name) {
            if (name == null) {
                return null;
            }
            return NAME_TO_OPERATION.get(name.toLowerCase());
        }

        private final String[] names;

        MongoOperation(String... names) {
            this.names = names;
        }
    }

    static class MongoStatement {
        private final String preparedStmt;

        private static Document readStmt(String preparedStmt) {
            return Document.parse(preparedStmt);
        }

        private final MongoOperation operation;
        private final String collection;
        private final Document query;
        private final Document value;
        private final Document projection;

        MongoStatement(DbStatementType dbStatementType, String preparedStmt) {
            this.preparedStmt = preparedStmt;
            Document jsonStmt = readStmt(preparedStmt);

            MongoOperation operation;
            if (jsonStmt.containsKey(JSON_OPERATION)) {
                operation = MongoOperation.operationByName(jsonStmt.getString(JSON_OPERATION));
                // make sure we have alignment between statement type and operation
                switch (dbStatementType) {
                    case QUERY, GET -> validateOperation(dbStatementType, operation, MongoOperation.QUERY);
                    case INSERT -> validateOperation(dbStatementType, operation, MongoOperation.INSERT);
                    case UPDATE -> validateOperation(dbStatementType, operation, MongoOperation.UPDATE);
                    case DELETE -> validateOperation(dbStatementType, operation, MongoOperation.DELETE);
                    case DML -> validateOperation(dbStatementType, operation, MongoOperation.INSERT,
                            MongoOperation.UPDATE, MongoOperation.DELETE);
                    case COMMAND -> validateOperation(dbStatementType, operation, MongoOperation.COMMAND);
                    default -> throw new IllegalStateException(
                            "Operation type is not defined in statement, and cannot be inferred from statement type: "
                                    + dbStatementType);
                }
            } else {
                operation = switch (dbStatementType) {
                    case QUERY, GET -> MongoOperation.QUERY;
                    case INSERT -> MongoOperation.INSERT;
                    case UPDATE -> MongoOperation.UPDATE;
                    case DELETE -> MongoOperation.DELETE;
                    case COMMAND -> MongoOperation.COMMAND;
                    default -> throw new IllegalStateException(
                            "Operation type is not defined in statement, and cannot be inferred from statement type: "
                                    + dbStatementType);
                };
            }
            this.operation = operation;
            this.collection = jsonStmt.getString(JSON_COLLECTION);
            this.value = jsonStmt.get(JSON_VALUE, Document.class);
            this.query = jsonStmt.get(JSON_QUERY, Document.class);
            this.projection = jsonStmt.get(JSON_PROJECTION, Document.class);
        }

        private static void validateOperation(DbStatementType dbStatementType,
                                              MongoOperation actual,
                                              MongoOperation... expected) {

            // PERF: time complexity of this check is terrible
            for (MongoOperation operation : expected) {
                if (actual == operation) {
                    return;
                }
            }

            throw new IllegalStateException("Statement type is "
                                                    + dbStatementType
                                                    + ", yet operation in statement is: "
                                                    + actual);
        }

        MongoOperation getOperation() {
            return operation;
        }

        String getCollection() {
            return collection;
        }

        Document getQuery() {
            return query != null ? query : EMPTY;
        }

        Document getValue() {
            return value;
        }

        Document getProjection() {
            return projection;
        }

        @Override
        public String toString() {
            return preparedStmt;
        }
    }

}
