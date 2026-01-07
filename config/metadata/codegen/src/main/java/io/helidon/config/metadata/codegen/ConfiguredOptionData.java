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

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.RoundContext;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.config.metadata.codegen.ConfiguredProperty.AllowedValue;

import static io.helidon.common.types.ElementKind.ENUM;
import static io.helidon.common.types.ElementKind.ENUM_CONSTANT;
import static io.helidon.config.metadata.codegen.TypeHandler.UNCONFIGURED_OPTION;
import static java.util.function.Predicate.not;

/**
 * Mirror of {@link Types#OPTION}.
 *
 * @param configured    configured
 * @param key           key, {@code null}{@index} if not set
 * @param type          type
 * @param description   description, {@code null} if not set
 * @param required      required
 * @param defaultValue  default value, {@code null} if not set
 * @param experimental  experimental
 * @param provider      provider
 * @param providerType  provider type, {@code null} if not set
 * @param deprecated    deprecated
 * @param merge         merge
 * @param kind          kind, {@code null} if not set
 * @param allowedValues allowed values
 */
record ConfiguredOptionData(
        boolean configured,
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
        List<AllowedValue> allowedValues) {

    /**
     * Create a new instance.
     *
     * @param ctx    codegen context
     * @param rc     round context
     * @param option annotation
     * @return ConfiguredOptionData
     */
    static ConfiguredOptionData create(CodegenContext ctx, RoundContext rc, Annotation option) {
        var typeInfo = option.typeValue("type")
                .flatMap(ctx::typeInfo)
                .orElse(null);
        List<AllowedValue> allowedValues;
        if (typeInfo != null && typeInfo.kind() == ENUM) {
            allowedValues = typeInfo.elementInfo().stream()
                    .filter(it -> it.kind() == ENUM_CONSTANT)
                    .map(it -> {
                        var description = JavadocProcessor.process(rc, typeInfo, it, true);
                        return new AllowedValue(it.elementName(), description);
                    })
                    .toList();
        } else {
            allowedValues = option.annotationValues("allowedValues")
                    .or(() -> option.annotationValue("allowedValue").map(List::of))
                    .stream()
                    .flatMap(List::stream)
                    .map(AllowedValue::create)
                    .toList();
        }

        return new ConfiguredOptionData(
                option.booleanValue("configured").orElse(true),
                option.stringValue("key").filter(not(String::isBlank)).orElse(null),
                option.typeValue("type").orElse(null),
                option.stringValue("description").filter(not(String::isBlank)).orElse(null),
                option.booleanValue("required").orElse(false),
                option.stringValue().filter(not(UNCONFIGURED_OPTION::equals)).orElse(null),
                option.booleanValue("experimental").orElse(false),
                option.booleanValue("provider").orElse(false),
                option.typeValue("providerType").orElse(null),
                option.booleanValue("deprecated").orElse(false),
                option.booleanValue("mergeWithParent").orElse(false),
                option.stringValue("kind").orElse("VALUE"),
                allowedValues);
    }

    /**
     * Get the enum values for an enum type.
     *
     * @param rc       round context
     * @param typeInfo type info
     * @return list of values
     */
    static List<AllowedValue> enumValues(RoundContext rc, TypeInfo typeInfo) {
        return typeInfo.elementInfo()
                .stream()
                .filter(it -> it.kind() == ENUM_CONSTANT)
                .map(it -> new AllowedValue(it.elementName(), it.description()
                        .map(javadoc -> JavadocProcessor.process(rc, typeInfo, it, true))
                        .orElse("")))
                .toList();
    }
}
