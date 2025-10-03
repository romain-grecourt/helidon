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
package io.helidon.tests.integration.dbclient.h2;

import io.helidon.dbclient.DbClient;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest.HttpObservabilityTest;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

@ServerTest
final class H2ObservabilityLocalTestIT implements ObservabilityTest, HttpObservabilityTest {

    private final ObservabilityTest delegate = new ObservabilityTest.TestImpl();
    private final HttpObservabilityTest httpDelegate;

    H2ObservabilityLocalTestIT(Http1Client client) {
        this.httpDelegate = new HttpObservabilityTest.TestImpl(client);
    }

    @SetUpRoute
    static void setUp(HttpRouting.Builder routing) {
        Main.routing(routing);
    }

    @AfterAll
    static void tearDown(DbClient db) {
        H2LocalTest.shutdown(db);
    }

    @Test
    @Override
    public void testHttpHealthNoDetails() {
        httpDelegate.testHttpHealthNoDetails();
    }

    @Test
    @Override
    public void testHttpHealthDetails() {
        httpDelegate.testHttpHealthDetails();
    }

    @Test
    @Override
    public void testHttpMetrics() {
        httpDelegate.testHttpMetrics();
    }

    @Test
    @Override
    public void testHealthCheck() {
        delegate.testHealthCheck();
    }

    @Test
    @Override
    public void testHealthCheckWithName() {
        delegate.testHealthCheckWithName();
    }

    @Override
    @SuppressWarnings("ALL")
    public void testHealthCheckWithCustomNamedDML() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("ALL")
    public void testHealthCheckWithCustomDML() {
        throw new UnsupportedOperationException();
    }

    @Test
    @Override
    public void testHealthCheckWithCustomNamedQuery() {
        delegate.testHealthCheckWithCustomNamedQuery();
    }

    @Test
    @Override
    public void testHealthCheckWithCustomQuery() {
        delegate.testHealthCheckWithCustomQuery();
    }
}
