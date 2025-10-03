package io.helidon.tests.integration.dbclient.common;

import io.helidon.dbclient.DbClient;
import io.helidon.http.BadRequestException;
import io.helidon.service.registry.Services;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;

/**
 * CRUD Http service.
 */
public class PokemonService implements HttpService {

    private static final JsonBuilderFactory JSON_FACTORY = Json.createBuilderFactory(null);

    private final DbClient db = Services.get(DbClient.class);

    @Override
    public void routing(HttpRules rules) {
        rules.get("/", this::list)
                .post("/", this::create);
    }

    private void list(ServerRequest req, ServerResponse res) {
        res.send(db.execute().namedQuery("select-pokemons")
                .map(it -> JSON_FACTORY.createObjectBuilder()
                        .add("id", it.column("id").getString())
                        .add("name", it.column("name").getString()))
                .collect(JSON_FACTORY::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build());
    }

    private void create(ServerRequest req, ServerResponse res) {
        JsonObject jsonObject = req.content().as(JsonObject.class);
        JsonNumber id = jsonObject.getJsonNumber("id");
        String name = jsonObject.getString("name", null);
        if (id == null || name == null) {
            throw new BadRequestException("Invalid request");
        }
        db.execute().namedInsert("insert-pokemon", id.intValue(), name);
        res.status(201);
        res.send();
    }
}
