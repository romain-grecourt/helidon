/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.pgsql;

import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

@ServerTest
final class PostgreSQLObservabilityLocalTestIT extends PostgreSQLLocalTest implements ObservabilityTest {

    private final ObservabilityTest delegate;

    PostgreSQLObservabilityLocalTestIT(Http1Client client) {
        delegate = new ObservabilityTest.TestImpl(client);
    }

    @Test
    @Override
    public void testHttpHealthNoDetails() {
        delegate.testHttpHealthNoDetails();
    }

    @Test
    @Override
    public void testHttpHealthDetails() {
        delegate.testHttpHealthDetails();
    }

    @Test
    @Override
    public void testHttpMetrics() {
        delegate.testHttpMetrics();
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
