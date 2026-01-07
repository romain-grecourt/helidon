/*
 * Copyright (c) 2026 Oracle and/or its affiliates
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
package io.helidon.config.metadata.model;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import io.helidon.config.metadata.model.ConfigMetadataImpl.CmAllowedValueImpl;
import io.helidon.config.metadata.model.ConfigMetadataImpl.CmModuleImpl;
import io.helidon.config.metadata.model.ConfigMetadataImpl.CmOptionImpl;
import io.helidon.config.metadata.model.ConfigMetadataImpl.CmTypeImpl;
import io.helidon.metadata.hson.Hson;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link ConfigMetadata}.
 */
class ConfigMetadataTest {

    @Test
    void testFromJson() {
        var actual = ConfigMetadata.fromJson(parseJson());
        var modules = actual.modules();
        assertThat(modules.size(), is(1));

        var module = modules.getFirst();
        var types = module.types();
        assertThat(module.module(), is("io.helidon.config.metadata.model"));
        assertThat(types.size(), is(1));

        var type = types.getFirst();
        assertThat(type.type(), is("io.helidon.config.metadata.model.AcmeConfig"));
        assertThat(type.shortType(), is("AcmeConfig"));
        assertThat(type.annotatedType(), is("io.helidon.config.metadata.model.AcmeConfig"));
        assertThat(type.description(), is(Optional.empty()));
        assertThat(type.prefix(), is(Optional.of("acme")));
        assertThat(type.standalone(), is(false));
        assertThat(type.inherits(), is(List.of()));
        assertThat(type.producers(), is(hasItems(
                "io.helidon.config.metadata.model.AcmeConfig#create(io.helidon.config.Config)",
                "io.helidon.config.metadata.model.AcmeConfig#builder()")));
        assertThat(type.provides(), hasItems(
                "io.helidon.config.metadata.model.AcmeProvider"));

        var options = type.options();
        assertThat(options.size(), is(1));

        var option = options.getFirst();
        assertThat(option.key(), is("option1"));
        assertThat(option.description(), is("The first option."));
        assertThat(option.method(), is("io.helidon.config.metadata.model.AcmeConfig.Builder#option1(long)"));
        assertThat(option.type(), is(Optional.of("java.lang.Long")));
        assertThat(option.defaultValue(), is(Optional.of("8192")));
        assertThat(option.required(), is(false));
        assertThat(option.experimental(), is(false));
        assertThat(option.deprecated(), is(false));
        assertThat(option.provider(), is(false));
        assertThat(option.providerType(), is(Optional.empty()));
        assertThat(option.merge(), is(false));
        assertThat(option.kind(), is(Optional.empty()));

        var allowedValues = option.allowedValues();
        assertThat(allowedValues.size(), is(1));

        var allowedValue = allowedValues.getFirst();
        assertThat(allowedValue.value(), is("8192"));
        assertThat(allowedValue.description(), is(Optional.of("eight one nine two")));
    }

    @Test
    void testToJson() {
        var actual = testMetadata().toJson();
        var expected = parseJson();
        assertThat(formatJson(actual), is(formatJson(expected)));
    }

    @Test
    void testLoadAll() throws Exception {
        var url = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().toURL();
        try (var cl = new URLClassLoader(new URL[] {url}, null)) {
            var actual = ConfigMetadata.loadAll(cl);
            var expected = testMetadata();
            assertThat(actual, is(expected));
        }
    }

    static ConfigMetadata testMetadata() {
        return new ConfigMetadataImpl(List.of(
                new CmModuleImpl(
                        "io.helidon.config.metadata.model",
                        List.of(
                                new CmTypeImpl(
                                        "io.helidon.config.metadata.model.AcmeConfig",
                                        "io.helidon.config.metadata.model.AcmeConfig",
                                        List.of(new CmOptionImpl(
                                                "option1",
                                                Optional.of("The first option."),
                                                "io.helidon.config.metadata.model.AcmeConfig.Builder#option1(long)",
                                                Optional.of("java.lang.Long"),
                                                Optional.of("8192"),
                                                false,
                                                false,
                                                false,
                                                false,
                                                Optional.empty(),
                                                false,
                                                Optional.empty(),
                                                List.of(
                                                        new CmAllowedValueImpl("8192", Optional.of("eight one nine two"))
                                                ))),
                                        Optional.empty(),
                                        Optional.of("acme"),
                                        false,
                                        List.of(),
                                        List.of("io.helidon.config.metadata.model.AcmeConfig#create(io.helidon.config.Config)",
                                                "io.helidon.config.metadata.model.AcmeConfig#builder()"),
                                        List.of("io.helidon.config.metadata.model.AcmeProvider"))
                        ))), new HashMap<>(), new HashMap<>());
    }

    static Hson.Array parseJson() {
        var is = ConfigMetadataTest.class.getResourceAsStream("/" + ConfigMetadata.LOCATION);
        assertThat(is, is(not(nullValue())));
        return Hson.parse(is).asArray();
    }

    static String formatJson(Hson.Array jsonArray) {
        var baos = new ByteArrayOutputStream();
        try (var printer = new PrintWriter(baos)) {
            jsonArray.write(printer, true);
        }
        return baos.toString();
    }
}
