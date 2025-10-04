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

import io.helidon.config.Config;
import io.helidon.service.registry.Services;
import io.helidon.tests.integration.dbclient.common.TestFactories;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest;
import io.helidon.tests.integration.dbclient.common.tests.ObservabilityTest.HttpObservabilityTest;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@ServerTest
final class MySQLObservabilityLocalTestIT extends MySQLLocalTest implements ObservabilityTest,
                                                                            HttpObservabilityTest {

    @Container
    static final MySQLContainer<?> CONTAINER = MySQLTestContainer.CONTAINER;

    private final ObservabilityTest delegate = new ObservabilityTest.TestImpl();
    private final HttpObservabilityTest httpDelegate;

    MySQLObservabilityLocalTestIT(Http1Client client) {
        this.httpDelegate = new HttpObservabilityTest.TestImpl(client);
    }

    @BeforeAll
    static void setUp() {
        Services.set(Config.class, TestFactories.config(MySQLTestContainer.config()));
    }

    @SetUpServer
    static void setUpRoutes(WebServerConfig.Builder server) {
        Main.setup(server);
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
