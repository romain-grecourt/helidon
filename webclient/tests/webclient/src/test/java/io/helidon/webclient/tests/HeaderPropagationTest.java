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

package io.helidon.webclient.tests;

import java.nio.charset.StandardCharsets;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static io.helidon.http.Method.POST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class HeaderPropagationTest {
    private static final HeaderName LATE_HEADER = HeaderNames.create("X-Late-Header");

    private final WebClient client;

    HeaderPropagationTest(WebServer server) {
        this.client = WebClient.builder()
                .baseUri("http://localhost:" + server.port())
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder router) {
        router.route(POST, "/late-header",
                     (req, res) -> {
                         String entity = req.content().as(String.class);
                         String lateHeader = req.headers().get(LATE_HEADER).get();
                         String contentLength = req.headers().get(HeaderNames.CONTENT_LENGTH).get();
                         res.send(lateHeader + ":" + contentLength + ":" + entity);
                     });
    }

    @Test
    void testLateHeadersPropagateToHttp1Delegate() {
        HttpClientRequest request = client.post()
                .path("/late-header")
                .protocolId(Http1Client.PROTOCOL_ID);

        try (var response = request.outputStream(output -> {
            request.header(HeaderValues.create(LATE_HEADER, "late"));
            request.header(HeaderValues.create(HeaderNames.CONTENT_LENGTH, "4"));
            output.write("data".getBytes(StandardCharsets.UTF_8));
            output.close();
        })) {
            assertThat(response.as(String.class), is("late:4:data"));
        }
    }
}
