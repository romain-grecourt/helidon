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
package io.helidon.config.metadata.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.metadata.hson.Hson;

import static java.util.function.Predicate.not;

/**
 * Configured property.
 *
 * @param method        method, never {@code null}
 * @param key           key, never {@code null}
 * @param type          type, never {@code null}
 * @param description   description
 * @param required      required
 * @param defaultValue  default value, {@code null} if unset
 * @param experimental  experimental
 * @param provider      provider
 * @param providerType  provider type, {@code null} if {@link #provider} if {@code false}
 * @param deprecated    deprecated
 * @param merge         merge
 * @param kind          kind
 * @param allowedValues allowed values
 * @param nestedType    nested type, may be {@code null}
 */
record ConfiguredProperty(
        String method,
        String key,
        TypeName type,
        String description,
        boolean required,
        String defaultValue,
        boolean experimental,
        boolean provider,
        TypeName providerType,
        boolean deprecated,
        boolean merge,
        String kind,
        List<AllowedValue> allowedValues,
        ConfiguredType nestedType) {

    /**
     * Copy this property with a fully  qualified key.
     *
     * @param fqKey fully qualified key
     * @return ConfiguredProperty
     */
    ConfiguredProperty flatten(String fqKey) {
        return new ConfiguredProperty(
                method,
                fqKey,
                type,
                description,
                required,
                defaultValue,
                experimental,
                provider,
                providerType,
                deprecated,
                merge,
                kind,
                allowedValues,
                null);
    }

    /**
     * Convert to JSON.
     *
     * @return JSON
     */
    Hson.Struct toJson() {
        var builder = Hson.Struct.builder();
        builder.set("key", key);
        if (!TypeNames.STRING.equals(type)) {
            builder.set("type", type.fqName());
        }
        builder.set("description", description);
        if (defaultValue != null) {
            builder.set("defaultValue", defaultValue);
        }
        if (experimental) {
            builder.set("experimental", true);
        }
        if (required) {
            builder.set("required", true);
        }
        if (!kind().equals("VALUE")) {
            builder.set("kind", kind);
        }
        if (provider) {
            builder.set("provider", true);
        }
        if (providerType != null) {
            builder.set("providerType", providerType.fqName());
        }
        if (deprecated) {
            builder.set("deprecated", true);
        }
        if (merge) {
            builder.set("merge", true);
        }
        if (method != null) {
            builder.set("method", method);
        }
        if (!allowedValues.isEmpty()) {
            builder.setStructs("allowedValues", allowedValues.stream()
                    .map(AllowedValue::toJson)
                    .toList());
        }
        return builder.build();
    }

    /**
     * Allowed value.
     *
     * @param value       value
     * @param description description
     */
    record AllowedValue(String value, String description) {

        /**
         * Create a new instance.
         *
         * @param annotation annotation
         * @return AllowedValue
         */
        static AllowedValue create(Annotation annotation) {
            var value = annotation.stringValue().orElseThrow();
            var description = annotation.stringValue("description").filter(not(String::isBlank)).orElse(null);
            return new AllowedValue(value, description);
        }

        /**
         * Convert to JSON.
         *
         * @return JSON
         */
        Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            builder.set("value", value);
            if (description != null && !description.isBlank()) {
                builder.set("description", description.trim());
            }
            return builder.build();
        }
    }
}
