/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.jersey.connector;

import java.io.IOException;
import java.net.URI;

import io.helidon.config.Config;
import io.helidon.jersey.webserver.JaxRsService;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class MultipartHeaderUpdateTest {
    private final Client client;
    private final URI uri;

    MultipartHeaderUpdateTest(URI uri) {
        var config = new ClientConfig();
        config.connectorProvider(HelidonConnectorProvider.create());
        config.register(MultiPartFeature.class);

        this.client = ClientBuilder.newClient(config);
        this.uri = uri;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        var resourceConfig = new ResourceConfig(MultipartResource.class)
                .register(MultiPartFeature.class);
        routing.register("/jaxrs", JaxRsService.create(Config.empty(), resourceConfig));
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void testMultipartBoundaryHeaderUpdatedBeforeSend() throws IOException {
        try (var multipart = new MultiPart()
                .bodyPart(new BodyPart("hello", MediaType.TEXT_PLAIN_TYPE));
             var response = client.target(uri)
                     .path("jaxrs/multipart")
                     .request()
                     .post(Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA_TYPE))) {
            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is("ok"));
        }
    }

    @Path("/multipart")
    public static class MultipartResource {
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String upload(MultiPart ignored) {
            return "ok";
        }
    }
}
