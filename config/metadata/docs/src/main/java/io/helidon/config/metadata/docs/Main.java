/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.helidon.logging.common.LogConfig;

/**
 * Main class to start generating config reference documentation.
 */
public final class Main {
    static {
        LogConfig.initClass();
    }

    private Main() {
    }

    /**
     * Start generating reference documentation.
     *
     * @param args either empty (heuristics), or a single parameter (the output directory)
     * @throws IllegalStateException if an error occurs
     */
    public static void main(String[] args) {
        LogConfig.configureRuntime();
        Path outputDir;
        if (args.length == 0) {
            try {
                var codeSource = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                outputDir = codeSource.resolve("../../../../docs/config");
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        } else if (args.length == 1) {
            outputDir = Paths.get(args[0]).toAbsolutePath().normalize();
        } else {
            throw new IllegalArgumentException("Invalid arguments, must be <= 1, got: " + args.length);
        }
        var docs = new ConfigDocs(outputDir, ConfigMetadata.loadAll());
        docs.process();
    }
}
