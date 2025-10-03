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
package io.helidon.tests.integration.dbclient.mysql;

import io.helidon.testing.junit5.Testing;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest;
import io.helidon.tests.integration.harness.ProcessRunner;
import io.helidon.tests.integration.harness.TestProcess;
import io.helidon.tests.integration.harness.TestProcesses;
import io.helidon.webclient.http1.Http1Client;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@TestProcesses
@Testing.Test
final class MySQLObservabilityRemoteTestIT implements ObservabilityTest {

    @Container
    static final MySQLContainer<?> CONTAINER = MySQLTestContainer.CONTAINER;

    @TestProcess
    static final ProcessRunner PROCESS_RUNNER = MySQLRemoteTest.PROCESS_RUNNER;

    private final ObservabilityTest delegate;

    @SuppressWarnings("resource")
    MySQLObservabilityRemoteTestIT() {
        String serverUrl = "http://localhost:%d".formatted(PROCESS_RUNNER.process().port());
        delegate = new ObservabilityTest.TestImpl(Http1Client.builder()
                .baseUri(serverUrl)
                .build());
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

    @Test
    @Override
    public void testHealthCheckWithCustomNamedDML() {
        delegate.testHealthCheckWithCustomNamedDML();
    }

    @Test
    @Override
    public void testHealthCheckWithCustomDML() {
        delegate.testHealthCheckWithCustomDML();
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
