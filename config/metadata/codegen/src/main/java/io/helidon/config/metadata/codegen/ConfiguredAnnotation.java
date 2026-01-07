/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
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

package io.helidon.config.metadata.codegen;

import java.util.List;
import java.util.function.Predicate;

import io.helidon.common.types.Annotation;

/**
 * Mirror of {@link Types#CONFIGURED}.
 *
 * @param description       description, {@code null} if unset
 * @param prefix            prefix, {@code null} if unset
 * @param provides          provides
 * @param root              root
 * @param ignoreBuildMethod ignoreBuildMethod
 */
record ConfiguredAnnotation(String description,
                            String prefix,
                            List<String> provides,
                            boolean root,
                            boolean ignoreBuildMethod) {

    /**
     * Create a new instance.
     *
     * @param annotation annotation
     * @return ConfiguredAnnotation
     */
    static ConfiguredAnnotation create(Annotation annotation) {
        return new ConfiguredAnnotation(
                annotation.stringValue("description").orElse(null),
                annotation.stringValue("prefix").filter(Predicate.not(String::isBlank)).orElse(null),
                annotation.stringValues("provides").orElseGet(List::of),
                annotation.booleanValue("root").orElse(false),
                annotation.booleanValue("ignoreBuildMethod").orElse(false));
    }
}
