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
package io.helidon.tests.integration.webclient;

import java.net.URI;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.UriHelper;
import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.WebClientService;
import io.helidon.nima.webserver.WebServer;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests propagation of the request parts defined in WebClientService.
 */
public class WebclientServiceValuePropagationTest extends TestParent {

    WebclientServiceValuePropagationTest(WebServer server, Http1Client client) {
        super(server, client);
    }

    @Test
    public void testUriPartsPropagation() {
        Http1Client webClient = Http1Client.builder()
                                           .baseUri("http://invalid")
                                           .service(new UriChangingService())
                                           .build();

        String response = webClient.get()
                .path("replace/me")
                .request(String.class);

        assertThat(response, is("Hi Test"));
    }

    @Test
    public void testInvalidSchema() {
        Http1Client webClient = Http1Client.builder()
                .baseUri("http://localhost:80")
                .service(new InvalidSchemaService())
                .build();

        RuntimeException exception = assertThrows(RuntimeException.class,
                                                     () -> webClient.get().request(String.class));

        assertThat(exception.getMessage(), is("invalid transport protocol is not supported!"));

    }

    private static final class UriChangingService implements WebClientService {

        @Override
        public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
            request.headers().set(Http.Header.HOST, "localhost");
            request.uri(UriHelper.create(URI.create("/greet/valuesPropagated"), null));
            request.query().add("param", "Hi");
            request.fragment("Test");
            return chain.proceed(request);
        }
    }

    private static final class InvalidSchemaService implements WebClientService {

        @Override
        public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
//            request.schema("invalid");
            return chain.proceed(request);
        }
    }

}
