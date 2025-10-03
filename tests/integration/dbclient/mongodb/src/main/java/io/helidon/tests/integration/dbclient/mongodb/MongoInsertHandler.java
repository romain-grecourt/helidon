/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import io.helidon.dbclient.DbRow;
import io.helidon.tests.integration.dbclient.common.tests.InsertHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MongoInsertHandler implements InsertHandler {
    @Override
    public void testInsertNamedArgsReturnedKeys(DbRow row) {
        assertThat(columnsCount(row), is(1));
        assertThat(row.column("_id"), is(row.column(1)));
    }

    @Override
    public void testInsertNamedArgsReturnedColumns(DbRow row) {
        assertThat(columnsCount(row), is(2));
        assertThat(row.column("id"), is(row.column(1)));
        assertThat(row.column("red"), is(row.column(2)));
    }
}
