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
package io.helidon.examples.dbclient.common;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import io.helidon.common.http.Http;
import io.helidon.common.parameters.Parameters;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.nima.webserver.http.Handler;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;

import jakarta.json.JsonObject;

/**
 * Common methods that do not differ between JDBC and MongoDB.
 */
public abstract class AbstractPokemonService implements HttpService {
    private static final Logger LOGGER = Logger.getLogger(AbstractPokemonService.class.getName());

    private final DbClient dbClient;

    /**
     * Create a new pokemon service with a DB client.
     *
     * @param dbClient DB client to use for database operations
     */
    protected AbstractPokemonService(DbClient dbClient) {
        this.dbClient = dbClient;
    }


    @Override
    public void routing(HttpRules rules) {
        rules
                .get("/", this::listPokemons)
                // create new
                .put("/", Handler.create(Pokemon.class, this::insertPokemon))
                // update existing
                .post("/{name}/type/{type}", this::insertPokemonSimple)
                // delete all
                .delete("/", this::deleteAllPokemons)
                // get one
                .get("/{name}", this::getPokemon)
                // delete one
                .delete("/{name}", this::deletePokemon)
                // example of transactional API (local transaction only!)
                .put("/transactional", Handler.create(Pokemon.class, this::transactional))
                // update one (TODO this is intentionally wrong - should use JSON request, just to make it simple we use path)
                .put("/{name}/type/{type}", this::updatePokemonType);
    }

    /**
     * The DB client associated with this service.
     *
     * @return DB client instance
     */
    protected DbClient dbClient() {
        return dbClient;
    }

    /**
     * This method is left unimplemented to show differences between native statements that can be used.
     *
     * @param req  Server request
     * @param res Server response
     */
    protected abstract void deleteAllPokemons(ServerRequest req, ServerResponse res);

    /**
     * Insert new pokemon with specified name.
     *
     * @param pokemon pokemon request entity
     * @param res the server response
     */
    private void insertPokemon(Pokemon pokemon, ServerResponse res) {
        long count = dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(pokemon)
                .execute());
        res.send("Inserted: " + count + " values");
    }

    /**
     * Insert new pokemon with specified name.
     *
     * @param req  the server request
     * @param res the server response
     */
    private void insertPokemonSimple(ServerRequest req, ServerResponse res) {
        Parameters params = req.path().pathParameters();
        // Test Pokemon POJO mapper
        Pokemon pokemon = new Pokemon(params.value("name"), params.value("type"));

        long count = dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(pokemon)
                .execute());
        res.send("Inserted: " + count + " values");
    }

    /**
     * Get a single pokemon by name.
     *
     * @param req  server request
     * @param res server response
     */
    private void getPokemon(ServerRequest req, ServerResponse res) {
        String pokemonName = req.path().pathParameters().value("name");
        dbClient.execute(exec -> exec.namedGet("select-one", pokemonName))
                .ifPresentOrElse(it -> sendRow(it, res),
                        () -> sendNotFound(res, "Pokemon "
                                + pokemonName
                                + " not found"));
    }

    /**
     * Return JsonArray with all stored pokemons or pokemons with matching attributes.
     *
     * @param req  the server request
     * @param res the server response
     */
    private void listPokemons(ServerRequest req, ServerResponse res) {
        Stream<JsonObject> rows = dbClient.execute(exec -> exec.namedQuery("select-all"))
                                          .map(it -> it.as(JsonObject.class));

        // TODO
        res.send(rows);
    }

    /**
     * Update a pokemon.
     * Uses a transaction.
     *
     * @param req  the server request
     * @param res the server response
     */
    private void updatePokemonType(ServerRequest req, ServerResponse res) {
        Parameters params = req.path().pathParameters();
        final String name = params.value("name");
        final String type = params.value("type");

        long count = dbClient.execute(exec -> exec
                .createNamedUpdate("update")
                .addParam("name", name)
                .addParam("type", type)
                .execute());
        res.send("Updated: " + count + " values");
    }

    private void transactional(Pokemon pokemon, ServerResponse res) {
        long count = dbClient.transaction(tx -> tx
                .createNamedGet("select-for-update")
                .namedParam(pokemon)
                .execute()
                .map(dbRow -> tx.createNamedUpdate("update")
                                .namedParam(pokemon)
                                .execute())
                .orElse(0L));
        res.send("Updated " + count + " records");

    }

    /**
     * Delete pokemon with specified name (key).
     *
     * @param req  the server request
     * @param res the server response
     */
    private void deletePokemon(ServerRequest req, ServerResponse res) {
        final String name = req.path().pathParameters().value("name");
        long count = dbClient.execute(exec -> exec.namedDelete("delete", name));
        res.send("Deleted: " + count + " values");
    }

    /**
     * Send a 404 status code.
     *
     * @param res the server response
     * @param message  entity content
     */
    protected void sendNotFound(ServerResponse res, String message) {
        res.status(Http.Status.NOT_FOUND_404);
        res.send(message);
    }

    /**
     * Send a single DB row as JSON object.
     *
     * @param row      row as read from the database
     * @param res server response
     */
    protected void sendRow(DbRow row, ServerResponse res) {
        res.send(row.as(JsonObject.class));
    }

    /**
     * Send a 500 response code and a few details.
     *
     * @param throwable throwable that caused the issue
     * @param res  server response
     * @param <T>       type of expected response, will be always {@code null}
     * @return {@code Void} so this method can be registered as a lambda
     * with {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}
     */
    protected <T> T sendError(Throwable throwable, ServerResponse res) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        res.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }

}
