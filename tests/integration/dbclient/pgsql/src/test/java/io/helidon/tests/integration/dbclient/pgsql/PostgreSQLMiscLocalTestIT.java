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

import io.helidon.tests.integration.dbclient.common.tests.MiscTest;

import org.junit.jupiter.api.Test;

final class PostgreSQLMiscLocalTestIT extends PostgreSQLLocalTest implements MiscTest {

    private final MiscTest delegate = new MiscTest.TestImpl();

    @Test
    @Override
    public void testFlowControl() {
        delegate.testFlowControl();
    }

    @Test
    @Override
    public void testStatementInterceptor() {
        delegate.testStatementInterceptor();
    }

    @Test
    @Override
    public void testInsertWithOrderMapping() {
        delegate.testInsertWithOrderMapping();
    }

    @Test
    @Override
    public void testInsertWithNamedMapping() {
        delegate.testInsertWithNamedMapping();
    }

    @Test
    @Override
    public void testUpdateWithOrderMapping() {
        delegate.testUpdateWithOrderMapping();
    }

    @Test
    @Override
    public void testUpdateWithNamedMapping() {
        delegate.testUpdateWithNamedMapping();
    }

    @Test
    @Override
    public void testDeleteWithOrderMapping() {
        delegate.testDeleteWithOrderMapping();
    }

    @Test
    @Override
    public void testDeleteWithNamedMapping() {
        delegate.testDeleteWithNamedMapping();
    }

    @Test
    @Override
    public void testQueryWithMapping() {
        delegate.testQueryWithMapping();
    }

    @Test
    @Override
    public void testGetWithMapping() {
        delegate.testGetWithMapping();
    }
}
