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
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbClientException;
import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbMapperManager;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbStatementQuery;
import io.helidon.dbclient.DbStatementType;
import io.helidon.dbclient.DbClientContext;
import io.helidon.dbclient.DbStatementContext;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Implementation of a query for MongoDB.
 */
public class MongoDbStatementQuery extends MongoDbStatement<DbStatementQuery, Stream<DbRow>> implements DbStatementQuery {
    private static final System.Logger LOGGER = System.getLogger(MongoDbStatementQuery.class.getName());

    MongoDbStatementQuery(MongoDatabase db, DbStatementContext statementContext) {
        super(db, statementContext);
    }

    @Override
    protected Stream<DbRow> doExecute(DbClientServiceContext dbContext) {
        String statement = build();

        MongoStatement stmt;
        try {
            stmt = queryOrCommand(statement);
        } catch (Exception e) {
            throw new DbClientException(e.getMessage(), e);
        }

        if (stmt.getOperation() != MongoDbStatement.MongoOperation.QUERY) {
            if (stmt.getOperation() == MongoDbStatement.MongoOperation.COMMAND) {
                return MongoDbCommandExecutor.executeCommand(this);
            }
            throw new UnsupportedOperationException(String.format(
                    "Operation %s is not supported by query", stmt.getOperation().toString()));
        }

        MongoStatement usedStatement = stmt;
        return callStatement(usedStatement);
    }

    private MongoStatement queryOrCommand(String statement) {
        try {
            return new MongoStatement(DbStatementType.QUERY, statement);
        } catch (IllegalStateException e) {
            // maybe this is a command?
            try {
                return new MongoStatement(DbStatementType.COMMAND, statement);
            } catch (IllegalStateException ignored) {
                // we want to report the original exception
                throw e;
            }
        }
    }

    private Stream<DbRow> callStatement(MongoStatement stmt) {
        final MongoCollection<Document> mc = db().getCollection(stmt.getCollection());
        final Document query = stmt.getQuery();
        final Document projection = stmt.getProjection();

        LOGGER.log(Level.DEBUG, () -> String.format(
                "Query: %s, Projection: %s",
                query.toString(),
                (projection != null ? projection : "N/A")));

        FindIterable<Document> finder = mc.find(query);
        if (projection != null) {
            finder = finder.projection(projection);
        }

        DbClientContext clientContext = clientContext();
        DbMapperManager dbMapperManager = clientContext.dbMapperManager();
        MapperManager mapperManager = clientContext.mapperManager();

        Spliterator<Document> spliterator = spliteratorUnknownSize(finder.iterator(), Spliterator.ORDERED);
        Stream<Document> stream = StreamSupport.stream(spliterator, false);
        return stream.map(doc -> MongoDbRow.create(doc, dbMapperManager, mapperManager));
    }
}
