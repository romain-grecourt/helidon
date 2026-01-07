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

package io.helidon.config.metadata.docs;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.helidon.config.metadata.model.ConfigMetadata;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link @ConfigDocs}.
 */
class ConfigDocsTest {

    @Test
    void testStandalone() throws Exception {
        var uri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        Path targetDir = Paths.get(uri).getParent();

        // ensure unique directory
        var outputDir = targetDir.resolve("config-docs-ut/standalone");
        for (int i = 1; Files.exists(outputDir); i++) {
            outputDir = targetDir.resolve("config-docs-ut" + "-" + i + "/standalone");
        }

        // generate docs
        try (var cl = new URLClassLoader(new URL[] {uri.toURL()}, null)) {
            var metadata = ConfigMetadata.loadAll(cl);
            new ConfigDocs(outputDir, metadata).process();
        }

        // load actual content
        var typeFile = outputDir.resolve("io.helidon.config.metadata.docs.AcmeConfig.md");
        assertThat(Files.exists(typeFile), is(true));
        var actual = Files.readString(typeFile);

        // load expected content
        var is = getClass().getResourceAsStream("/expected/standalone/AcmeConfig.md");
        assertThat(is, is(not(nullValue())));
        var expected = new String(is.readAllBytes());

        // compare
        assertThat(actual, is(expected));
    }
}
