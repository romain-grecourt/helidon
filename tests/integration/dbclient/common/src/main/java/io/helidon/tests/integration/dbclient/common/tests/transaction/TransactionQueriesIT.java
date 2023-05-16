/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.dbclient.common.tests.transaction;

import java.util.List;

import io.helidon.dbclient.DbRow;
import io.helidon.tests.integration.dbclient.common.AbstractIT;

import org.junit.jupiter.api.Test;

import static io.helidon.tests.integration.dbclient.common.utils.Utils.verifyPokemon;

/**
 * Test set of basic JDBC queries in transaction.
 */
public class TransactionQueriesIT extends AbstractIT {

    /**
     * Verify {@code createNamedQuery(String, String)} API method with ordered parameters.
     */
    @Test
    public void testCreateNamedQueryStrStrOrderArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .createNamedQuery("select-pikachu", SELECT_POKEMON_ORDER_ARG)
                .addParam(POKEMONS.get(1).getName())
                .execute()
                .toList());
        verifyPokemon(rows, POKEMONS.get(1));
    }

    /**
     * Verify {@code createNamedQuery(String)} API method with named parameters.
     */
    @Test
    public void testCreateNamedQueryStrNamedArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .createNamedQuery("select-pokemon-named-arg")
                .addParam("name", POKEMONS.get(2).getName())
                .execute()
                .toList());
        verifyPokemon(rows, POKEMONS.get(2));
    }

    /**
     * Verify {@code createNamedQuery(String)} API method with ordered parameters.
     */
    @Test
    public void testCreateNamedQueryStrOrderArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .createNamedQuery("select-pokemon-order-arg")
                .addParam(POKEMONS.get(3).getName())
                .execute()
                .toList());
        verifyPokemon(rows, POKEMONS.get(3));
    }

    /**
     * Verify {@code createQuery(String)} API method with named parameters.
     */
    @Test
    public void testCreateQueryNamedArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .createQuery(SELECT_POKEMON_NAMED_ARG)
                .addParam("name", POKEMONS.get(4).getName())
                .execute()
                .toList());
        verifyPokemon(rows, POKEMONS.get(4));
    }

    /**
     * Verify {@code createQuery(String)} API method with ordered parameters.
     */
    @Test
    public void testCreateQueryOrderArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .createQuery(SELECT_POKEMON_ORDER_ARG)
                .addParam(POKEMONS.get(5).getName())
                .execute()
                .toList());
        verifyPokemon(rows, POKEMONS.get(5));
    }

    /**
     * Verify {@code namedQuery(String)} API method with ordered parameters passed directly to the {@code namedQuery} method.
     */
    @Test
    public void testNamedQueryOrderArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .namedQuery("select-pokemon-order-arg", POKEMONS.get(6).getName())
                .toList());
        verifyPokemon(rows, POKEMONS.get(6));
    }

    /**
     * Verify {@code query(String)} API method with ordered parameters passed directly to the {@code query} method.
     */
    @Test
    public void testQueryOrderArgs() {
        List<DbRow> rows = DB_CLIENT.transaction(exec -> exec
                .query(SELECT_POKEMON_ORDER_ARG, POKEMONS.get(7).getName())
                .toList());
        verifyPokemon(rows, POKEMONS.get(7));
    }

}
