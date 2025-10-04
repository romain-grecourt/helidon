/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

import java.lang.reflect.InvocationTargetException;

import io.helidon.http.InternalServerException;
import io.helidon.http.NotFoundException;
import io.helidon.tests.integration.dbclient.common.tests.InsertHandler;
import io.helidon.tests.integration.dbclient.common.tests.MiscTest;
import io.helidon.tests.integration.dbclient.common.tests.SimpleTest;
import io.helidon.tests.integration.dbclient.common.tests.StatementTest;
import io.helidon.tests.integration.dbclient.common.tests.TransactionTest;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import static java.util.Objects.requireNonNullElse;

/**
 * Http service to invoke the tests remotely.
 */
public final class TestService implements HttpService {

    private final TransactionTest.TestImpl transaction;
    private final StatementTest.TestImpl statement;
    private final SimpleTest.TestImpl simple;
    private final MiscTest.TestImpl misc;

    /**
     * Create a new instance.
     */
    public TestService(InsertHandler insertHandler) {
        transaction = new TransactionTest.TestImpl(insertHandler);
        simple = new SimpleTest.TestImpl(insertHandler);
        statement = new StatementTest.TestImpl();
        misc = new MiscTest.TestImpl();
    }

    @Override
    public void routing(HttpRules rules) {
        rules
                .get("/transaction/{testName}", this::transaction)
                .get("/statement/{testName}", this::statement)
                .get("/simple/{testName}", this::simple)
                .get("/misc/{testName}", this::misc);
    }

    private void transaction(ServerRequest req, ServerResponse res) {
        invokeTest(transaction, req, res);
    }

    private void statement(ServerRequest req, ServerResponse res) {
        invokeTest(statement, req, res);
    }

    private void simple(ServerRequest req, ServerResponse res) {
        invokeTest(simple, req, res);
    }

    private void misc(ServerRequest req, ServerResponse res) {
        invokeTest(misc, req, res);
    }

    private static void invokeTest(Object o, ServerRequest req, ServerResponse res) {
        String testName = req.path().pathParameters().get("testName");
        try {
            o.getClass().getMethod(testName).invoke(o);
            res.send("OK");
        } catch (IllegalAccessException ex) {
            throw new InternalServerException(ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            requireNonNullElse(ex.getCause(), ex).printStackTrace(System.err);
            throw new InternalServerException(ex.getMessage(), ex);
        } catch (NoSuchMethodException ignored) {
            throw new NotFoundException("Test not found: " + testName);
        }
    }
}
