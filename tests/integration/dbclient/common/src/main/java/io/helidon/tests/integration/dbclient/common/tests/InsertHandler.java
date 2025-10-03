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
package io.helidon.tests.integration.dbclient.common.tests;

import io.helidon.dbclient.DbRow;

/**
 * Database specific handler.
 */
public interface InsertHandler {

    /**
     * Verify {@code namedInsert(String)} API method with named parameters and returned generated keys.
     */
    void testInsertNamedArgsReturnedKeys(DbRow row);

    /**
     * Verify {@code namedInsert(String)} API method with named parameters and returned insert columns.
     */
    default void testInsertNamedArgsReturnedColumns(DbRow row) {
        testInsertNamedArgsReturnedKeys(row);
    }

    /**
     * Get the columns count.
     *
     * @param row row
     * @return count
     */
    default int columnsCount(DbRow row) {
        int[] count = {0};
        row.forEach(col -> count[0]++);
        return count[0];
    }
}
