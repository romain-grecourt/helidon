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
package io.helidon.tests.integration.dbclient.oracle;

import io.helidon.tests.integration.dbclient.common.tests.StatementTest;

import org.junit.jupiter.api.Test;

final class OracleStatementLocalTestIT extends OracleLocalTest implements StatementTest {

    private final StatementTest delegate = new StatementTest.TestImpl();

    @Test
    @Override
    public void testCreateNamedQueryNonExistentStmt() {
        delegate.testCreateNamedQueryNonExistentStmt();
    }

    @Test
    @Override
    public void testCreateNamedQueryNamedAndOrderArgsWithoutArgs() {
        delegate.testCreateNamedQueryNamedAndOrderArgsWithoutArgs();
    }

    @Test
    @Override
    public void testCreateNamedQueryNamedAndOrderArgsWithArgs() {
        delegate.testCreateNamedQueryNamedAndOrderArgsWithArgs();
    }

    @Test
    @Override
    public void testCreateNamedQueryNamedArgsSetOrderArg() {
        delegate.testCreateNamedQueryNamedArgsSetOrderArg();
    }

    @Test
    @Override
    public void testCreateNamedQueryOrderArgsSetNamedArg() {
        delegate.testCreateNamedQueryOrderArgsSetNamedArg();
    }

    @Test
    @Override
    public void testGetArrayParams() {
        delegate.testGetArrayParams();
    }

    @Test
    @Override
    public void testGetListParams() {
        delegate.testGetListParams();
    }

    @Test
    @Override
    public void testGetMapParams() {
        delegate.testGetMapParams();
    }

    @Test
    @Override
    public void testGetOrderParam() {
        delegate.testGetOrderParam();
    }

    @Test
    @Override
    public void testGetNamedParam() {
        delegate.testGetNamedParam();
    }

    @Test
    @Override
    public void testGetMappedNamedParam() {
        delegate.testGetMappedNamedParam();
    }

    @Test
    @Override
    public void testGetMappedOrderParam() {
        delegate.testGetMappedOrderParam();
    }

    @Test
    @Override
    public void testQueryArrayParams() {
        delegate.testQueryArrayParams();
    }

    @Test
    @Override
    public void testQueryListParams() {
        delegate.testQueryListParams();
    }

    @Test
    @Override
    public void testQueryMapParams() {
        delegate.testQueryMapParams();
    }

    @Test
    @Override
    public void testQueryMapMissingParams() {
        delegate.testQueryMapMissingParams();
    }

    @Test
    @Override
    public void testQueryOrderParam() {
        delegate.testQueryOrderParam();
    }

    @Test
    @Override
    public void testQueryNamedParam() {
        delegate.testQueryNamedParam();
    }

    @Test
    @Override
    public void testQueryMappedNamedParam() {
        delegate.testQueryMappedNamedParam();
    }

    @Test
    @Override
    public void testQueryMappedOrderParam() {
        delegate.testQueryMappedOrderParam();
    }

    @Test
    @Override
    public void testDmlArrayParams() {
        delegate.testDmlArrayParams();
    }

    @Test
    @Override
    public void testDmlListParams() {
        delegate.testDmlListParams();
    }

    @Test
    @Override
    public void testDmlMapParams() {
        delegate.testDmlMapParams();
    }

    @Test
    @Override
    public void testDmlOrderParam() {
        delegate.testDmlOrderParam();
    }

    @Test
    @Override
    public void testDmlNamedParam() {
        delegate.testDmlNamedParam();
    }

    @Test
    @Override
    public void testDmlMappedNamedParam() {
        delegate.testDmlMappedNamedParam();
    }

    @Test
    @Override
    public void testDmlMappedOrderParam() {
        delegate.testDmlMappedOrderParam();
    }
}
