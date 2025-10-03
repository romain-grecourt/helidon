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

import io.helidon.tests.integration.dbclient.common.tests.SimpleTest;

import org.junit.jupiter.api.Test;

final class MongoDBSimpleLocalTestIT extends MongoDBLocalTest implements SimpleTest {

    private final SimpleTest delegate = new SimpleTest.TestImpl(new MongoInsertHandler());

    @Test
    @Override
    public void testCreateNamedDeleteStrStrOrderArgs() {
        delegate.testCreateNamedDeleteStrStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedDeleteStrNamedArgs() {
        delegate.testCreateNamedDeleteStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDeleteStrOrderArgs() {
        delegate.testCreateNamedDeleteStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateDeleteNamedArgs() {
        delegate.testCreateDeleteNamedArgs();
    }

    @Test
    @Override
    public void testCreateDeleteOrderArgs() {
        delegate.testCreateDeleteOrderArgs();
    }

    @Test
    @Override
    public void testNamedDeleteOrderArgs() {
        delegate.testNamedDeleteOrderArgs();
    }

    @Test
    @Override
    public void testDeleteOrderArgs() {
        delegate.testDeleteOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithInsertStrStrNamedArgs() {
        delegate.testCreateNamedDmlWithInsertStrStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithInsertStrNamedArgs() {
        delegate.testCreateNamedDmlWithInsertStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithInsertStrOrderArgs() {
        delegate.testCreateNamedDmlWithInsertStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithInsertNamedArgs() {
        delegate.testCreateDmlWithInsertNamedArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithInsertOrderArgs() {
        delegate.testCreateDmlWithInsertOrderArgs();
    }

    @Test
    @Override
    public void testNamedDmlWithInsertOrderArgs() {
        delegate.testNamedDmlWithInsertOrderArgs();
    }

    @Test
    @Override
    public void testDmlWithInsertOrderArgs() {
        delegate.testDmlWithInsertOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithUpdateStrStrNamedArgs() {
        delegate.testCreateNamedDmlWithUpdateStrStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithUpdateStrNamedArgs() {
        delegate.testCreateNamedDmlWithUpdateStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithUpdateStrOrderArgs() {
        delegate.testCreateNamedDmlWithUpdateStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithUpdateNamedArgs() {
        delegate.testCreateDmlWithUpdateNamedArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithUpdateOrderArgs() {
        delegate.testCreateDmlWithUpdateOrderArgs();
    }

    @Test
    @Override
    public void testNamedDmlWithUpdateOrderArgs() {
        delegate.testNamedDmlWithUpdateOrderArgs();
    }

    @Test
    @Override
    public void testDmlWithUpdateOrderArgs() {
        delegate.testDmlWithUpdateOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithDeleteStrStrOrderArgs() {
        delegate.testCreateNamedDmlWithDeleteStrStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithDeleteStrNamedArgs() {
        delegate.testCreateNamedDmlWithDeleteStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedDmlWithDeleteStrOrderArgs() {
        delegate.testCreateNamedDmlWithDeleteStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithDeleteNamedArgs() {
        delegate.testCreateDmlWithDeleteNamedArgs();
    }

    @Test
    @Override
    public void testCreateDmlWithDeleteOrderArgs() {
        delegate.testCreateDmlWithDeleteOrderArgs();
    }

    @Test
    @Override
    public void testNamedDmlWithDeleteOrderArgs() {
        delegate.testNamedDmlWithDeleteOrderArgs();
    }

    @Test
    @Override
    public void testDmlWithDeleteOrderArgs() {
        delegate.testDmlWithDeleteOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedGetStrStrNamedArgs() {
        delegate.testCreateNamedGetStrStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedGetStrNamedArgs() {
        delegate.testCreateNamedGetStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedGetStrOrderArgs() {
        delegate.testCreateNamedGetStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateGetNamedArgs() {
        delegate.testCreateGetNamedArgs();
    }

    @Test
    @Override
    public void testCreateGetOrderArgs() {
        delegate.testCreateGetOrderArgs();
    }

    @Test
    @Override
    public void testNamedGetStrOrderArgs() {
        delegate.testNamedGetStrOrderArgs();
    }

    @Test
    @Override
    public void testGetStrOrderArgs() {
        delegate.testGetStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedInsertStrStrNamedArgs() {
        delegate.testCreateNamedInsertStrStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedInsertStrNamedArgs() {
        delegate.testCreateNamedInsertStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedInsertStrOrderArgs() {
        delegate.testCreateNamedInsertStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateInsertNamedArgs() {
        delegate.testCreateInsertNamedArgs();
    }

    @Test
    @Override
    public void testCreateInsertOrderArgs() {
        delegate.testCreateInsertOrderArgs();
    }

    @Test
    @Override
    public void testNamedInsertOrderArgs() {
        delegate.testNamedInsertOrderArgs();
    }

    @Test
    @Override
    public void testInsertOrderArgs() {
        delegate.testInsertOrderArgs();
    }

    @Test
    @Override
    public void testInsertNamedArgsReturnedKeys() throws Exception {
        delegate.testInsertNamedArgsReturnedKeys();
    }

    @Test
    @Override
    public void testInsertNamedArgsReturnedColumns() throws Exception {
        delegate.testInsertNamedArgsReturnedColumns();
    }

    @Test
    @Override
    public void testCreateNamedQueryStrStrOrderArgs() {
        delegate.testCreateNamedQueryStrStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedQueryStrNamedArgs() {
        delegate.testCreateNamedQueryStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedQueryStrOrderArgs() {
        delegate.testCreateNamedQueryStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateQueryNamedArgs() {
        delegate.testCreateQueryNamedArgs();
    }

    @Test
    @Override
    public void testCreateQueryOrderArgs() {
        delegate.testCreateQueryOrderArgs();
    }

    @Test
    @Override
    public void testNamedQueryOrderArgs() {
        delegate.testNamedQueryOrderArgs();
    }

    @Test
    @Override
    public void testQueryOrderArgs() {
        delegate.testQueryOrderArgs();
    }

    @Test
    @Override
    public void testCreateNamedUpdateStrStrNamedArgs() {
        delegate.testCreateNamedUpdateStrStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedUpdateStrNamedArgs() {
        delegate.testCreateNamedUpdateStrNamedArgs();
    }

    @Test
    @Override
    public void testCreateNamedUpdateStrOrderArgs() {
        delegate.testCreateNamedUpdateStrOrderArgs();
    }

    @Test
    @Override
    public void testCreateUpdateNamedArgs() {
        delegate.testCreateUpdateNamedArgs();
    }

    @Test
    @Override
    public void testCreateUpdateOrderArgs() {
        delegate.testCreateUpdateOrderArgs();
    }

    @Test
    @Override
    public void testNamedUpdateNamedArgs() {
        delegate.testNamedUpdateNamedArgs();
    }

    @Test
    @Override
    public void testUpdateOrderArgs() {
        delegate.testUpdateOrderArgs();
    }
}
