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
package io.helidon.tests.integration.dbclient.common.tests.simple;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;

import io.helidon.dbclient.DbExecute;
import io.helidon.tests.integration.dbclient.common.AbstractIT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.tests.integration.dbclient.common.utils.Utils.verifyDeletePokemon;
import static io.helidon.tests.integration.dbclient.common.utils.Utils.verifyInsertPokemon;

/**
 * Test set of basic JDBC delete calls.
 */
@SuppressWarnings("SpellCheckingInspection")
public class SimpleDeleteIT extends AbstractIT {

    /**
     * Local logger instance.
     */
    private static final System.Logger LOGGER = System.getLogger(SimpleDeleteIT.class.getName());

    /**
     * Maximum Pokemon ID.
     */
    private static final int BASE_ID = LAST_POKEMON_ID + 30;

    /**
     * Map of pokemons for update tests.
     */
    private static final Map<Integer, Pokemon> POKEMONS = new HashMap<>();

    private static void addPokemon(Pokemon pokemon) {
        POKEMONS.put(pokemon.getId(), pokemon);
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec.namedInsert("insert-pokemon", pokemon.getId(), pokemon.getName());
            verifyInsertPokemon(result, pokemon);
        }
    }

    /**
     * Initialize tests of basic JDBC deletes.
     */
    @BeforeAll
    public static void setup() {
        try {
            int curId = BASE_ID;
            addPokemon(new Pokemon(++curId, "Rayquaza", TYPES.get(3), TYPES.get(16))); // BASE_ID+1
            addPokemon(new Pokemon(++curId, "Lugia", TYPES.get(3), TYPES.get(14)));    // BASE_ID+2
            addPokemon(new Pokemon(++curId, "Ho-Oh", TYPES.get(3), TYPES.get(10)));    // BASE_ID+3
            addPokemon(new Pokemon(++curId, "Raikou", TYPES.get(13)));                 // BASE_ID+4
            addPokemon(new Pokemon(++curId, "Giratina", TYPES.get(8), TYPES.get(16))); // BASE_ID+5
            addPokemon(new Pokemon(++curId, "Regirock", TYPES.get(6)));                // BASE_ID+6
            addPokemon(new Pokemon(++curId, "Kyogre", TYPES.get(11)));                 // BASE_ID+7
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, String.format("Exception in setup: %s", ex));
            throw ex;
        }
    }


    /**
     * Verify {@code createNamedDelete(String, String)} API method with ordered parameters.
     */
    @Test
    public void testCreateNamedDeleteStrStrOrderArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDelete("delete-rayquaza", DELETE_POKEMON_ORDER_ARG)
                    .addParam(POKEMONS.get(BASE_ID + 1).getId()).execute();
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 1));
        }
    }

    /**
     * Verify {@code createNamedDelete(String)} API method with named parameters.
     */
    @Test
    public void testCreateNamedDeleteStrNamedArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDelete("delete-pokemon-named-arg")
                    .addParam("id", POKEMONS.get(BASE_ID + 2).getId()).execute();
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 2));
        }
    }

    /**
     * Verify {@code createNamedDelete(String)} API method with ordered parameters.
     */
    @Test
    public void testCreateNamedDeleteStrOrderArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDelete("delete-pokemon-order-arg")
                    .addParam(POKEMONS.get(BASE_ID + 3).getId()).execute();
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 3));
        }
    }

    /**
     * Verify {@code createDelete(String)} API method with named parameters.
     */
    @Test
    public void testCreateDeleteNamedArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createDelete(DELETE_POKEMON_NAMED_ARG)
                    .addParam("id", POKEMONS.get(BASE_ID + 4).getId()).execute();
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 4));
        }
    }

    /**
     * Verify {@code createDelete(String)} API method with ordered parameters.
     */
    @Test
    public void testCreateDeleteOrderArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createDelete(DELETE_POKEMON_ORDER_ARG)
                    .addParam(POKEMONS.get(BASE_ID + 5).getId()).execute();
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 5));
        }
    }

    /**
     * Verify {@code namedDelete(String)} API method with ordered parameters.
     */
    @Test
    public void testNamedDeleteOrderArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .namedDelete("delete-pokemon-order-arg", POKEMONS.get(BASE_ID + 6).getId());
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 6));
        }
    }

    /**
     * Verify {@code delete(String)} API method with ordered parameters.
     */
    @Test
    public void testDeleteOrderArgs() {
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .delete(DELETE_POKEMON_ORDER_ARG, POKEMONS.get(BASE_ID + 7).getId());
            verifyDeletePokemon(result, POKEMONS.get(BASE_ID + 7));
        }
    }

}
