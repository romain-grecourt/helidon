/*
 * Copyright (c) 2021, 2026 Oracle and/or its affiliates.
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
import java.util.stream.Collectors;

import io.helidon.common.types.TypeName;
import io.helidon.metadata.hson.Hson;

/**
 * Configured type.
 *
 * @param annotation     annotation
 * @param targetClass    target class (runtime type)
 * @param annotatedClass annotated class
 * @param properties     properties
 * @param producers      producers
 * @param inherits       effective type hierarchy
 */
record ConfiguredType(
        ConfiguredAnnotation annotation,
        TypeName targetClass,
        TypeName annotatedClass,
        Set<ConfiguredProperty> properties,
        List<ProducerMethod> producers,
        List<TypeName> inherits) {

    /**
     * Convert to JSON.
     *
     * @return JSON
     */
    Hson.Struct toJson() {
        var builder = Hson.Struct.builder();

        builder.set("type", targetClass.fqName());
        builder.set("annotatedType", annotatedClass.fqName());
        if (annotation.root()) {
            builder.set("standalone", true);
        }
        if (annotation.prefix() != null) {
            builder.set("prefix", annotation.prefix());
        }
        if (annotation.description() != null) {
            builder.set("description", annotation.description());
        }

        if (!inherits.isEmpty()) {
            builder.setStrings("inherits", inherits.stream()
                    .map(TypeName::fqName)
                    .toList());
        }

        if (!annotation.provides().isEmpty()) {
            builder.setStrings("provides", annotation.provides());
        }

        if (!producers.isEmpty()) {
            builder.setStrings("producers", producers.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        }

        // flatten all properties
        var allProperties = new ArrayList<ConfiguredProperty>();
        flatten(allProperties, annotation.prefix(), properties);

        builder.setStructs("options", allProperties.stream()
                .map(ConfiguredProperty::toJson)
                .toList());
        return builder.build();
    }

    /**
     * The method that declares the option.
     *
     * @param owningClass  class
     * @param methodName   method name
     * @param methodParams method parameters
     */
    record ProducerMethod(TypeName owningClass, String methodName, List<TypeName> methodParams) {
    }

    private static void flatten(List<ConfiguredProperty> result, String prefix, Set<ConfiguredProperty> properties) {
        for (var p : properties) {
            var fqKey = prefix == null ? p.key() : prefix + "." + p.key();
            if (p.nestedType() != null) {
                flatten(result, fqKey, p.nestedType().properties);
            } else {
                result.add(p.flatten(fqKey));
            }
        }
    }
}
