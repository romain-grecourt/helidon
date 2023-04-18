/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.nima.graphql.server;

import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.nima.http.media.jsonb.JsonbSupport;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.http.media.MediaContext;
import io.helidon.nima.webclient.WebClient;
import io.helidon.nima.webclient.http1.Http1Client;

import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class GraphQlServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void testHelloWorld() {
        WebServer server = WebServer.builder()
                                    .host("localhost")
                                    .routing(r -> r.register(GraphQlService.create(buildSchema())))
                                    .build()
                                    .start();

        try {
            Http1Client webClient =
                    WebClient.builder()
                             .mediaContext(MediaContext.builder()
                                                       .addMediaSupport(JsonbSupport.create())
                                                       .build())
                             .build();

            LinkedHashMap<String, Object> response =
                    webClient.post()
                             .uri("http://localhost:" + server.port() + "/graphql")
                             .submit("{\"query\": \"{hello}\"}")
                             .as(LinkedHashMap.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat("POST errors: " + response.get("errors"), data, notNullValue());
            assertThat("POST", data.get("hello"), is("world"));

            response = webClient.get()
                                .uri("http://localhost:" + server.port() + "/graphql")
                                .queryParam("query", "{hello}")
                                .request(LinkedHashMap.class);

            data = (Map<String, Object>) response.get("data");
            assertThat("GET errors: " + response.get("errors"), data, notNullValue());
            assertThat("GET", data.get("hello"), is("world"));
        } finally {
            server.stop();
        }
    }

    private static GraphQLSchema buildSchema() {
        String schema = "type Query{hello: String}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring =
                RuntimeWiring.newRuntimeWiring()
                             .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
                             .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }
}
