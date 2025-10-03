/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

package io.helidon.webserver.testing.junit5;

import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@RoutingTest
class TestRoutingParameters {

    static Http1Client beforeAllClient;
    Http1Client beforeEachClient;

    @SetUpRoute
    static void setUpRoutes(HttpRouting.Builder routing) {
        routing.get((req, res) -> res.send("OK"));
    }

    @BeforeAll
    static void beforeAll(Http1Client client) {
        beforeAllClient = client;
    }

    @BeforeEach
    void beforeEach(Http1Client client) {
        beforeEachClient = client;
    }

    @Test
    void testBeforeAllParameter() {
        assertThat(beforeAllClient, is(not(nullValue())));
        try (var res = beforeAllClient.get().request()) {
            assertThat(res.status().code(), is(200));
            assertThat(res.entity().as(String.class), is("OK"));
        }
    }

    @Test
    void testBeforeEachParameter() {
        assertThat(beforeEachClient, is(not(nullValue())));
        try (var res = beforeAllClient.get().request()) {
            assertThat(res.status().code(), is(200));
            assertThat(res.entity().as(String.class), is("OK"));
        }
    }

    @Test
    void testMethodParameter(Http1Client client) {
        assertThat(client, is(not(nullValue())));
        try (var res = client.get().request()) {
            assertThat(res.status().code(), is(200));
            assertThat(res.entity().as(String.class), is("OK"));
        }
    }
}
