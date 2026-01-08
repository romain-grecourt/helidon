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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import io.helidon.config.metadata.model.CmModel;
import io.helidon.metadata.hson.Hson;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link @ConfigDocs}.
 */
class CmDocCodegenTest {

    @Test
    void test1() throws Exception {
        var classesDir = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        var targetDir = classesDir.getParent();

        // ensure unique directory
        var outputDir = targetDir.resolve("config-docs-ut/test1");
        for (int i = 1; Files.exists(outputDir); i++) {
            outputDir = targetDir.resolve("config-docs-ut" + "-" + i + "/test1");
        }

        // generate docs
        var is = Files.newInputStream(classesDir.resolve("config-metadata.json"));
        var metadata = CmModel.fromJson(Hson.parse(is).asArray());
        new CmDocCodegen(outputDir, metadata).process();

        // verify content
        try (Stream<Path> stream = Files.list(classesDir)
                .filter(it -> it.getFileName().toString().endsWith(".md"))) {

            var expectedFiles = stream.toList();
            for (var expectedFile : expectedFiles) {

                var actualFile = outputDir.resolve(expectedFile.getFileName());
                assertThat(Files.exists(actualFile), is(true));

                var expected = Files.readString(expectedFile);
                var actual = Files.readString(actualFile);
                assertThat(actual, is(expected));
            }
        }
    }
}
