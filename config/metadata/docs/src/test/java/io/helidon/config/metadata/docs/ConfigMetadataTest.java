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
package io.helidon.config.metadata.docs;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

class ConfigMetadataTest {

    @Test
    void testParse() {
        var is = getClass().getResourceAsStream("/config-metadata.json");
        assertThat(is, is(not(nullValue())));

        var metadata = new ConfigMetadata(is);
        assertThat(metadata.modules(), is(List.of(
                new ConfigMetadata.CmModule(
                        "io.helidon.config.metadata.docs",
                        List.of(
                                new ConfigMetadata.CmType(
                                        "io.helidon.config.metadata.docs.AcmeConfig",
                                        "io.helidon.config.metadata.docs.AcmeConfig",
                                        List.of(new ConfigMetadata.CmOption(
                                                "option1",
                                                "The first option.",
                                                "io.helidon.config.metadata.docs.AcmeConfig.Builder#option1(long)",
                                                "java.lang.Long",
                                                "8192",
                                                false,
                                                false,
                                                false,
                                                false,
                                                null,
                                                false,
                                                ConfigMetadata.CmOption.Kind.VALUE,
                                                List.of())),
                                        null,
                                        "acme",
                                        false,
                                        List.of(),
                                        List.of("io.helidon.config.metadata.docs.AcmeConfig#create(io.helidon.config.Config)",
                                                "io.helidon.config.metadata.docs.AcmeConfig#builder()"),
                                        List.of("io.helidon.config.metadata.docs.AcmeProvider"))
                        ))
        )));
    }
}
