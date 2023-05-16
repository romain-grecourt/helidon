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
package io.helidon.tests.integration.dbclient.common.tests.statement;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.dbclient.DbExecute;
import io.helidon.tests.integration.dbclient.common.AbstractIT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.tests.integration.dbclient.common.utils.Utils.verifyInsertPokemon;
import static io.helidon.tests.integration.dbclient.common.utils.Utils.verifyUpdatePokemon;

/**
 * Test DbStatementDml methods.
 */
@SuppressWarnings("SpellCheckingInspection")
public class DmlStatementIT extends AbstractIT {

    /**
     * Local logger instance.
     */
    private static final System.Logger LOGGER = System.getLogger(DmlStatementIT.class.getName());

    /**
     * Maximum Pokemon ID.
     */
    private static final int BASE_ID = LAST_POKEMON_ID + 100;

    /**
     * Map of pokemons for DbStatementDml methods tests.
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
     * Initialize DbStatementDml methods tests.
     */
    @BeforeAll
    public static void setup() {
        try {
            addPokemon(new Pokemon(BASE_ID, "Shinx", TYPES.get(13)));                      // BASE_ID+0
            addPokemon(new Pokemon(BASE_ID + 1, "Luxio", TYPES.get(13)));               // BASE_ID+1
            addPokemon(new Pokemon(BASE_ID + 2, "Luxray", TYPES.get(13)));              // BASE_ID+2
            addPokemon(new Pokemon(BASE_ID + 3, "Kricketot", TYPES.get(7)));            // BASE_ID+3
            addPokemon(new Pokemon(BASE_ID + 4, "Kricketune", TYPES.get(7)));           // BASE_ID+4
            addPokemon(new Pokemon(BASE_ID + 5, "Phione", TYPES.get(11)));              // BASE_ID+5
            addPokemon(new Pokemon(BASE_ID + 6, "Chatot", TYPES.get(1), TYPES.get(3))); // BASE_ID+6
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, String.format("Exception in setup: %s", ex), ex);
            throw ex;
        }
    }

    /**
     * Verify {@code params(Object... parameters)} parameters setting method.
     */
    @Test
    public void testDmlArrayParams() {
        Pokemon pokemon = new Pokemon(BASE_ID, "Luxio", TYPES.get(13));
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-order-arg")
                    .params(pokemon.getName(), pokemon.getId())
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code params(List<?>)} parameters setting method.
     */
    @Test
    public void testDmlListParams() {
        Pokemon pokemon = new Pokemon(BASE_ID + 1, "Luxray", TYPES.get(13));
        List<Object> params = new ArrayList<>(2);
        params.add(pokemon.getName());
        params.add(pokemon.getId());
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-order-arg")
                    .params(params)
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code params(Map<?>)} parameters setting method.
     */
    @Test
    public void testDmlMapParams() {
        Pokemon pokemon = new Pokemon(BASE_ID + 2, "Shinx", TYPES.get(13));
        Map<String, Object> params = new HashMap<>(2);
        params.put("name", pokemon.getName());
        params.put("id", pokemon.getId());
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-named-arg")
                    .params(params)
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code addParam(Object parameter)} parameters setting method.
     */
    @Test
    public void testDmlOrderParam() {
        Pokemon pokemon = new Pokemon(BASE_ID + 3, "Kricketune", TYPES.get(7));
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-order-arg")
                    .addParam(pokemon.getName())
                    .addParam(pokemon.getId())
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code addParam(String name, Object parameter)} parameters setting method.
     */
    @Test
    public void testDmlNamedParam() {
        Pokemon pokemon = new Pokemon(BASE_ID + 4, "Kricketot", TYPES.get(7));
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-named-arg")
                    .addParam("name", pokemon.getName())
                    .addParam("id", pokemon.getId())
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code namedParam(Object parameters)} mapped parameters setting method.
     */
    @Test
    public void testDmlMappedNamedParam() {
        Pokemon pokemon = new Pokemon(BASE_ID + 5, "Chatot", TYPES.get(1), TYPES.get(3));
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-named-arg")
                    .namedParam(pokemon)
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

    /**
     * Verify {@code indexedParam(Object parameters)} mapped parameters setting method.
     */
    @Test
    public void testDmlMappedOrderParam() {
        Pokemon pokemon = new Pokemon(BASE_ID + 6, "Phione", TYPES.get(11));
        try (DbExecute exec = DB_CLIENT.execute()) {
            long result = exec
                    .createNamedDmlStatement("update-pokemon-order-arg")
                    .indexedParam(pokemon)
                    .execute();
            verifyUpdatePokemon(result, pokemon);
        }
    }

}
