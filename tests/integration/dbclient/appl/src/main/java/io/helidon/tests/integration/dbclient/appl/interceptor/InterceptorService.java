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
package io.helidon.tests.integration.dbclient.appl.interceptor;

import java.util.Map;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbClientService;
import io.helidon.dbclient.DbClientServiceContext;
import io.helidon.dbclient.DbExecute;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.tests.integration.dbclient.appl.AbstractService;
import io.helidon.tests.integration.dbclient.appl.model.Pokemon;
import io.helidon.tests.integration.tools.service.AppResponse;
import io.helidon.tests.integration.tools.service.RemoteTestException;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import static io.helidon.tests.integration.tools.service.AppResponse.exceptionStatus;

/**
 * Web resource to verify services handling.
 */
public class InterceptorService extends AbstractService {

    private final DbClientService interceptor;

    /**
     * Creates an instance of web resource to verify services handling.
     *
     * @param dbClient    DbClient instance
     * @param statements  statements from configuration file
     * @param interceptor DbClientService interceptor instance used in test
     */
    public InterceptorService(DbClient dbClient, Map<String, String> statements, DbClientService interceptor) {
        super(dbClient, statements);
        this.interceptor = interceptor;
    }

    // Interceptor is registered in AppMain
    public static final class TestClientService implements DbClientService {

        private boolean called;
        private DbClientServiceContext context;

        public TestClientService() {
            this.called = false;
            this.context = null;
        }

        @Override
        public DbClientServiceContext statement(DbClientServiceContext context) {
            this.called = true;
            this.context = context;
            return context;
        }

        private boolean called() {
            return called;
        }

    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/testStatementInterceptor", this::testStatementInterceptor);
    }

    // Check that statement interceptor was called before statement execution.
    private void testStatementInterceptor(ServerRequest request, ServerResponse response) {
        try (DbExecute exec = dbClient().execute()) {
            JsonArrayBuilder jab = exec.createNamedQuery("select-pokemon-named-arg")
                                       .addParam("name", Pokemon.POKEMONS.get(6).getName())
                                       .execute()
                                       .map(row -> row.as(JsonObject.class))
                                       .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll);
            if (((TestClientService) interceptor).called()) {
                response.send(AppResponse.okStatus(jab.build()));
            } else {
                response.send(exceptionStatus(new RemoteTestException("Interceptor service was not called")));
            }
        }
    }

}
