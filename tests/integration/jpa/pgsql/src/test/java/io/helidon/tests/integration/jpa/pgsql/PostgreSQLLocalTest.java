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
package io.helidon.tests.integration.jpa.pgsql;

import java.util.Map;

import io.helidon.microprofile.testing.junit5.AddConfigMap;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for the local tests.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class PostgreSQLLocalTest {

    @Container
    static final JdbcDatabaseContainer<?> CONTAINER = PostgreSQLTestContainer.CONTAINER;

    @AddConfigMap
    static Map<String, String> config() {
        return PostgreSQLTestContainer.config();
    }
}
