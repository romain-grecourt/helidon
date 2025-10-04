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
package io.helidon.tests.integration.dbclient.mongodb;

import io.helidon.testing.junit5.Testing;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest.HttpObservabilityTest;
import io.helidon.tests.integration.harness.ProcessRunner;
import io.helidon.tests.integration.harness.TestProcess;
import io.helidon.tests.integration.harness.TestProcesses;
import io.helidon.webclient.http1.Http1Client;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestProcesses
@Testing.Test
final class MongoDBObservabilityRemoteTestIT implements HttpObservabilityTest {

    @Container
    static final GenericContainer<?> CONTAINER = MongoDBTestContainer.CONTAINER;

    @TestProcess
    static final ProcessRunner PROCESS_RUNNER = MongoDBRemoteTest.PROCESS_RUNNER;

    private final HttpObservabilityTest delegate;

    @SuppressWarnings("resource")
    MongoDBObservabilityRemoteTestIT() {
        String serverUrl = "http://localhost:%d".formatted(PROCESS_RUNNER.process().port());
        delegate = new HttpObservabilityTest.TestImpl(Http1Client.builder()
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
}
