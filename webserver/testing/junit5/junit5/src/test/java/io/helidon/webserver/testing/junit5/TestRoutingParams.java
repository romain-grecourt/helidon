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

import java.util.ArrayList;
import java.util.List;

import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

class TestRoutingParams {

    @Nested
    @RoutingTest
    class NoParamsTest {
        private static final List<String> EVENTS = new ArrayList<>();

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
            routing.get((req, res) -> res.send("OK"));
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach() {
            EVENTS.add("beforeEach");
        }

        @Test
        void testEventsOrderWithNoParams() {
            assertThat(EVENTS, is(List.of("beforeAll", "setUpRouting", "beforeEach")));
        }
    }

    @Nested
    @RoutingTest
    class ConstructorParamsTest {
        private static final List<String> EVENTS = new ArrayList<>();
        private final Http1Client client;

        ConstructorParamsTest(Http1Client client) {
            this.client = client;
        }

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
            routing.get((req, res) -> res.send("OK"));
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach() {
            EVENTS.add("beforeEach");
        }

        @Test
        void testMethodParams() {
            assertThat(EVENTS, is(List.of("beforeAll", "setUpRouting", "beforeEach")));
            assertThat(client, is(not(nullValue())));
            try (var res = client.get().request()) {
                assertThat(res.status().code(), is(200));
                assertThat(res.entity().as(String.class), is("OK"));
            }
        }
    }

    @Nested
    @RoutingTest
    class MethodParamsTest {
        private static final List<String> EVENTS = new ArrayList<>();

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
            routing.get((req, res) -> res.send("OK"));
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach() {
            EVENTS.add("beforeEach");
        }

        @Test
        void testMethodParams(Http1Client client) {
            assertThat(EVENTS, is(List.of("beforeAll", "setUpRouting", "beforeEach")));
            assertThat(client, is(not(nullValue())));
            try (var res = client.get().request()) {
                assertThat(res.status().code(), is(200));
                assertThat(res.entity().as(String.class), is("OK"));
            }
        }
    }

    @Nested
    @RoutingTest
    class BeforeEachParamsTest {
        private static final List<String> EVENTS = new ArrayList<>();
        private Http1Client client;

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
            routing.get((req, res) -> res.send("OK"));
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach(Http1Client c) {
            EVENTS.add("beforeEach");
            client = c;
        }

        @Test
        void testBeforeEachWithParams() {
            assertThat(EVENTS, is(List.of("beforeAll", "setUpRouting", "beforeEach")));
            assertThat(client, is(not(nullValue())));
            try (var res = client.get().request()) {
                assertThat(res.status().code(), is(200));
                assertThat(res.entity().as(String.class), is("OK"));
            }
        }
    }

    @Nested
    @RoutingTest
    class BeforeAllParamsTest {
        private static final List<String> EVENTS = new ArrayList<>();
        private static Http1Client client;

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
            routing.get((req, res) -> res.send("OK"));
        }

        @BeforeAll
        static void beforeAll(Http1Client c) {
            EVENTS.add("beforeAll");
            client = c;
        }

        @BeforeEach
        void beforeEach() {
            EVENTS.add("beforeEach");
        }

        @Test
        void testBeforeAllWithParams() {
            assertThat(EVENTS, is(List.of("setUpRouting", "beforeAll", "beforeEach")));
            assertThat(client, is(not(nullValue())));
            try (var res = client.get().request()) {
                assertThat(res.status().code(), is(200));
                assertThat(res.entity().as(String.class), is("OK"));
            }
        }
    }
}
