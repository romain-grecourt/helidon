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
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SuppressWarnings("ALL")
class TestRoutingHooksOrder {

    @Test
    void testOrder() {
        Events events = EngineTestKit.engine("junit-jupiter")
                .configurationParameter("TestRoutingHooksOrder", "true")
                .selectors(
                        selectClass(NoParametersTest.class),
                        selectClass(BeforeEachParametersTest.class),
                        selectClass(BeforeAllParametersTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats
                        .failed(0)
                        .succeeded(3));
    }

    @EnabledIfParameter(key = "TestRoutingHooksOrder", value = "true")
    @RoutingTest
    static class NoParametersTest {
        private static final List<String> EVENTS = new ArrayList<>();

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
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
        void testOrder() {
            assertThat(EVENTS, is(List.of("beforeAll", "beforeEach", "setUpRouting")));
        }
    }

    @EnabledIfParameter(key = "TestRoutingHooksOrder", value = "true")
    @RoutingTest
    static class BeforeEachParametersTest {
        private static final List<String> EVENTS = new ArrayList<>();

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach(Http1Client client) {
            EVENTS.add("beforeEach");
        }

        @Test
        void testOrder() {
            assertThat(EVENTS, is(List.of("beforeAll", "setUpRouting", "beforeEach")));
        }
    }

    @EnabledIfParameter(key = "TestRoutingHooksOrder", value = "true")
    @RoutingTest
    static class BeforeAllParametersTest {
        private static final List<String> EVENTS = new ArrayList<>();

        @SetUpRoute
        static void setUpRouting(HttpRouting.Builder routing) {
            EVENTS.add("setUpRouting");
        }

        @BeforeAll
        static void beforeAll() {
            EVENTS.add("beforeAll");
        }

        @BeforeEach
        void beforeEach(Http1Client client) {
            EVENTS.add("beforeEach");
        }

        @Test
        void testOrder() {
            assertThat(EVENTS, is(List.of("setUpRouting", "beforeAll", "beforeEach")));
        }
    }
}
