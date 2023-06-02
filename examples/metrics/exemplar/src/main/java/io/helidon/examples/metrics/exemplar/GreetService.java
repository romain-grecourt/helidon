/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.examples.metrics.exemplar;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.metrics.api.RegistryFactory;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;

/**
 * A simple service to greet you. Examples:
 * <p>
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 * <p>
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 * <p>
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 * <p>
 * The message is returned as a JSON object
 */

public class GreetService implements HttpService {

    /**
     * The config value for the key {@code greeting}.
     */
    private final AtomicReference<String> greeting = new AtomicReference<>();
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    static final String TIMER_FOR_GETS = "timerForGets";
    static final String COUNTER_FOR_PERSONALIZED_GREETINGS = "counterForPersonalizedGreetings";

    private final Timer timerForGets;
    private final Counter personalizedGreetingsCounter;

    GreetService(Config config) {
        greeting.set(config.get("app.greeting").asString().orElse("Ciao"));
        MetricRegistry registry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION);
        Metadata metadata = Metadata.builder()
                                    .withName(TIMER_FOR_GETS)
                                    .withUnit(MetricUnits.NANOSECONDS)
                                    .withType(MetricType.TIMER)
                                    .build();
        timerForGets = registry.timer(metadata);

        metadata = Metadata.builder()
                           .withName(COUNTER_FOR_PERSONALIZED_GREETINGS)
                           .withUnit(MetricUnits.NONE)
                           .withType(MetricType.COUNTER)
                           .build();
        personalizedGreetingsCounter = registry.counter(metadata);
    }

    /**
     * A service registers itself by updating the routing rules.
     *
     * @param rules the routing rules.
     */
    @Override
    public void routing(HttpRules rules) {
        rules.get("/", this::timeGet, this::getDefaultMessageHandler)
             .get("/{name}", this::countPersonalized, this::getMessageHandler)
             .put("/greeting", this::updateGreetingHandler);
    }

    /**
     * Return a worldly greeting message.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void getDefaultMessageHandler(ServerRequest request, ServerResponse response) {
        sendResponse(response, "World");
    }

    /**
     * Return a greeting message using the name that was provided.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void getMessageHandler(ServerRequest request, ServerResponse response) {
        String name = request.path().pathParameters().value("name");
        sendResponse(response, name);
    }

    private void sendResponse(ServerResponse response, String name) {
        String msg = String.format("%s %s!", greeting.get(), name);

        JsonObject returnObject = JSON.createObjectBuilder()
                                      .add("message", msg)
                                      .build();
        response.send(returnObject);
    }

    private void updateGreetingFromJson(JsonObject jo, ServerResponse response) {

        if (!jo.containsKey("greeting")) {
            JsonObject jsonErrorObject = JSON.createObjectBuilder()
                                             .add("error", "No greeting provided")
                                             .build();
            response.status(Http.Status.BAD_REQUEST_400)
                    .send(jsonErrorObject);
            return;
        }

        greeting.set(jo.getString("greeting"));
        response.status(Http.Status.NO_CONTENT_204).send();
    }

    /**
     * Set the greeting to use in future messages.
     *
     * @param request  the server request
     * @param response the server response
     */
    private void updateGreetingHandler(ServerRequest request, ServerResponse response) {
        JsonObject jsonObject = request.content().as(JsonObject.class);
        updateGreetingFromJson(jsonObject, response);
    }

    private void timeGet(ServerRequest request, ServerResponse response) {
        timerForGets.time((Runnable) response::next);
    }

    private void countPersonalized(ServerRequest request, ServerResponse response) {
        personalizedGreetingsCounter.inc();
        response.next();
    }
}
