/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.appl.transaction;

import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.DbTransaction;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.tests.integration.dbclient.appl.AbstractService;
import io.helidon.tests.integration.tools.service.RemoteTestException;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import static io.helidon.tests.integration.tools.service.AppResponse.exceptionStatus;
import static io.helidon.tests.integration.tools.service.AppResponse.okStatus;

/**
 * Web resource to test set of basic DbClient queries in transaction.
 */
public class TransactionQueriesService extends AbstractService {

    private static final System.Logger LOGGER = System.getLogger(TransactionQueriesService.class.getName());

    private interface TestFunction extends Function<String, Stream<DbRow>> {
    }

    public TransactionQueriesService(final DbClient dbClient, final Map<String, String> statements) {
        super(dbClient, statements);
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/testCreateNamedQueryStrStrOrderArgs", this::testCreateNamedQueryStrStrOrderArgs)
             .get("/testCreateNamedQueryStrNamedArgs", this::testCreateNamedQueryStrNamedArgs)
             .get("/testCreateNamedQueryStrOrderArgs", this::testCreateNamedQueryStrOrderArgs)
             .get("/testCreateQueryNamedArgs", this::testCreateQueryNamedArgs)
             .get("/testCreateQueryOrderArgs", this::testCreateQueryOrderArgs)
             .get("/testNamedQueryOrderArgs", this::testNamedQueryOrderArgs)
             .get("/testQueryOrderArgs", this::testQueryOrderArgs);
    }

    // Common test execution code
    private void executeTest(ServerRequest request,
                             ServerResponse response,
                             String testName,
                             TestFunction test) {
        LOGGER.log(Level.DEBUG, () -> String.format("Running SimpleQueryService.%s on server", testName));
        try {
            String name = param(request, QUERY_NAME_PARAM);
            JsonArray jsonArray = test.apply(name)
                                      .map(row -> row.as(JsonObject.class))
                                      .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                                      .build();
            response.send(okStatus(jsonArray));
        } catch (RemoteTestException ex) {
            LOGGER.log(Level.DEBUG, () -> String.format("Error in SimpleQueryService.%s on server", testName));
            response.send(exceptionStatus(ex));
        }
    }

    // Verify {@code createNamedQuery(String, String)} API method with ordered
    private void testCreateNamedQueryStrStrOrderArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testCreateNamedQueryStrStrOrderArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.createNamedQuery("select-pikachu", statement("select-pokemon-order-arg"))
                                 .addParam(name)
                                 .execute();
                    }
                });
    }

    // Verify {@code createNamedQuery(String)} API method with named parameters.
    private void testCreateNamedQueryStrNamedArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testCreateNamedQueryStrNamedArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.createNamedQuery("select-pokemon-named-arg")
                                 .addParam("name", name)
                                 .execute();
                    }
                });
    }

    // Verify {@code createNamedQuery(String)} API method with ordered
    private void testCreateNamedQueryStrOrderArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testCreateNamedQueryStrOrderArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.createNamedQuery("select-pokemon-order-arg")
                                 .addParam(name)
                                 .execute();
                    }
                });
    }

    // Verify {@code createQuery(String)} API method with named parameters.
    private void testCreateQueryNamedArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testCreateQueryNamedArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.createQuery(statement("select-pokemon-named-arg"))
                                 .addParam("name", name)
                                 .execute();
                    }
                });
    }

    // Verify {@code createQuery(String)} API method with ordered parameters.
    private void testCreateQueryOrderArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testCreateQueryOrderArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.createQuery(statement("select-pokemon-order-arg"))
                                 .addParam(name)
                                 .execute();
                    }
                });
    }

    // Verify {@code namedQuery(String)} API method with ordered parameters
    private void testNamedQueryOrderArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testNamedQueryOrderArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.namedQuery("select-pokemon-order-arg", name);
                    }
                });
    }

    // Verify {@code query(String)} API method with ordered parameters passed
    private void testQueryOrderArgs(final ServerRequest request, final ServerResponse response) {
        executeTest(request, response, "testQueryOrderArgs",
                name -> {
                    try (DbTransaction tx = dbClient().transaction()) {
                        return tx.query(statement("select-pokemon-order-arg"), name);
                    }
                });
    }

}
