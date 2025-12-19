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

package io.helidon.builder.tests.third.party.factory;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class LoggerPrototypeTest {
    @Test
    public void testConsumer() {
        // make sure the consumer methods for builders are generated
        var using = UsingConfig.builder()
                .logger(l -> l.name(LoggerPrototypeTest.First.class.getName()))
                .addBaseLogger(l -> l.name(LoggerPrototypeTest.First.class.getName()))
                .stringOption("Something")
                .build();

        assertThat(using.logger().getName(), is(LoggerPrototypeTest.First.class.getName()));
        assertThat(using.baseLoggers(), hasSize(1));
        assertThat(using.baseLoggers().getFirst().getName(), is(LoggerPrototypeTest.First.class.getName()));
    }

    @Test
    public void testFactoryPrototypeFromConfig() {
        Config config = Config.just(ConfigSources.classpath("/application.yaml"));
        var logger = LoggerConfig.create(config.get("test-1.logger"))
                .build();

        assertThat(logger, notNullValue());
        assertThat(logger.getName(), is("logger.name"));
    }

    @Test
    public void testUsingPrototypeFromConfig() {
        Config config = Config.just(ConfigSources.classpath("/application.yaml"));
        var using = UsingConfig.create(config.get("test-1"));

        assertThat(using, notNullValue());
        assertThat(using.stringOption(), is("string value"));
        assertThat(using.logger().getName(), is("logger.name"));
    }

    @Test
    public void testGeneratedMetadata() {
        var is = getClass().getResourceAsStream("/META-INF/helidon/config-metadata.json");
        assertThat(is, is(not(nullValue())));

        var reader = Json.createReader(is);
        var actual = reader.readArray();
        assertThat(actual, is(hasItem(allOf(
                hasString("module", is("io.helidon.builder.tests.third.party.factory")),
                hasArray("types", allOf(
                        hasItem(allOf(
                                hasString("type", is("io.helidon.builder.tests.third.party.factory.UsingConfig")),
                                hasString("description", is("<code>N/A</code>")))),
                        hasItem(allOf(
                                hasString("type", is("java.lang.System.Logger")),
                                hasString("description", is("Configuration object for <code>java.lang.System.Logger</code>"))))
                ))
        ))));
    }

    static Matcher<JsonValue> hasArray(String name, Matcher<JsonArray> matcher) {
        return new FeatureMatcher<>(matcher, "has property " + name, name) {
            @Override
            protected JsonArray featureValueOf(JsonValue target) {
                return target.asJsonObject().getJsonArray(name);
            }
        };
    }

    static Matcher<JsonValue> hasString(String name, Matcher<String> matcher) {
        return new FeatureMatcher<>(matcher, "has property " + name, name) {
            @Override
            protected String featureValueOf(JsonValue target) {
                return target.asJsonObject().getString(name);
            }
        };
    }

    static class First {
    }
}
