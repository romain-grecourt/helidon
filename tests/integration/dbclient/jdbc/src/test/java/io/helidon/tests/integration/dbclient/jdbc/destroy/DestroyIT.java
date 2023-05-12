/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.jdbc.destroy;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.tests.integration.dbclient.common.AbstractIT.DB_CLIENT;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Destroy database
 */
public class DestroyIT {

    /**
     * Delete database content.
     *
     * @param dbClient Helidon database client
     */
    private static void dropSchema(DbClient dbClient) {
        try (DbExecute exec = dbClient.execute()) {
            exec.namedDml("drop-poketypes");
            exec.namedDml("drop-pokemons");
            exec.namedDml("drop-types");
        }
    }


    /**
     * Destroy database after tests.
     */
    @BeforeAll
    public static void destroy() {
        try {
            dropSchema(DB_CLIENT);
        } catch (Throwable th) {
            fail("Database cleanup failed!", th);
        }
    }

    /**
     * Verify that table Types does not exist.
     */
    @Test
    void testTypesDeleted() {
        testTableNotExist("select-types");
    }

    /**
     * Verify that table Pokemons does not exist.
     */
    @Test
    public void testPokemonsDeleted() {
        testTableNotExist("select-pokemons");
    }

    /**
     * Verify that table PokemonTypes does not exist.
     */
    @Test
    public void testPokemonTypesDeleted() {
        testTableNotExist("select-poketypes-all");
    }

    private void testTableNotExist(String statementName) {
        try (DbExecute exec = DB_CLIENT.execute()) {
            exec.namedQuery(statementName).forEach(p -> {});
        }
    }
}
