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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.helidon.metadata.hson.Hson;

record ConfigMetadataImpl(List<CmModule> modules,
                          Map<String, CmType> typeCache,
                          Map<String, List<CmType>> providersCache) implements ConfigMetadata {

    ConfigMetadataImpl(Hson.Array array) {
        this(array.getStructs().stream().map(CmModule::fromJson).toList(),
                new HashMap<>(),
                new HashMap<>());
    }

    @Override
    public Optional<CmType> type(String typeName) {
        return Optional.ofNullable(typeCache.computeIfAbsent(typeName,
                k -> ConfigMetadata.super.type(k).orElse(null)));
    }

    @Override
    public List<CmType> providers(String typeName) {
        return providersCache.computeIfAbsent(typeName, ConfigMetadata.super::providers);
    }

    @Override
    public Hson.Array toJson() {
        return Hson.Array.create(modules.stream()
                .map(CmModule::toJson)
                .toList());
    }

    record CmModuleImpl(String module, List<CmType> types) implements CmModule {

        CmModuleImpl(Hson.Struct struct) {
            this(string(struct, "module"),
                    list(struct, "types", CmType::fromJson));
        }

        @Override
        public Hson.Struct toJson() {
            return Hson.structBuilder()
                    .set("module", module)
                    .setStructs("types", types.stream()
                            .map(CmType::toJson)
                            .toList())
                    .build();
        }
    }

    record CmTypeImpl(String type,
                      String annotatedType,
                      List<CmOption> options,
                      Optional<String> description,
                      Optional<String> prefix,
                      boolean standalone,
                      List<String> inherits,
                      List<String> producers,
                      List<String> provides) implements CmType {

        CmTypeImpl(Hson.Struct struct) {
            this(string(struct, "type"),
                    string(struct, "annotatedType"),
                    list(struct, "options", CmOption::fromJson),
                    struct.stringValue("description"),
                    struct.stringValue("prefix"),
                    struct.booleanValue("standalone").orElse(false),
                    list(struct, "inherits"),
                    list(struct, "producers"),
                    list(struct, "provides"));
        }

        @Override
        public Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            builder.set("type", type);
            builder.set("annotatedType", annotatedType);
            if (standalone) {
                builder.set("standalone", true);
            }
            description.ifPresent(it -> builder.set("description", it));
            prefix.ifPresent(it -> builder.set("prefix", it));
            if (!inherits.isEmpty()) {
                builder.setStrings("inherits", inherits);
            }
            if (!provides.isEmpty()) {
                builder.setStrings("provides", provides);
            }
            if (!producers.isEmpty()) {
                builder.setStrings("producers", producers);
            }
            builder.setStructs("options", options.stream()
                    .map(CmOption::toJson)
                    .toList());
            return builder.build();
        }
    }

    record CmOptionImpl(
            String key,
            Optional<String> description,
            String method,
            Optional<String> type,
            Optional<String> defaultValue,
            boolean required,
            boolean experimental,
            boolean deprecated,
            boolean provider,
            Optional<String> providerType,
            boolean merge,
            Optional<Kind> kind,
            List<CmAllowedValue> allowedValues) implements CmOption {

        CmOptionImpl(Hson.Struct struct) {
            this(string(struct, "key"),
                    struct.stringValue("description"),
                    string(struct, "method"),
                    struct.stringValue("type"),
                    struct.stringValue("defaultValue"),
                    struct.booleanValue("required").orElse(false),
                    struct.booleanValue("experimental").orElse(false),
                    struct.booleanValue("deprecated").orElse(false),
                    struct.booleanValue("provider").orElse(false),
                    struct.stringValue("providerType"),
                    struct.booleanValue("merge").orElse(false),
                    struct.stringValue("kind").map(Kind::valueOf),
                    list(struct, "allowedValues", CmAllowedValue::fromJson));
        }

        @Override
        public Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            builder.set("key", key);
            type.ifPresent(it -> builder.set("type", it));
            description.ifPresent(it -> builder.set("description", it));
            defaultValue.ifPresent(it -> builder.set("defaultValue", it));
            if (experimental) {
                builder.set("experimental", true);
            }
            if (required) {
                builder.set("required", true);
            }
            kind.ifPresent(it -> builder.set("kind", it.name()));
            if (provider) {
                builder.set("provider", true);
            }
            providerType.ifPresent(it -> builder.set("providerType", it));
            if (deprecated) {
                builder.set("deprecated", true);
            }
            if (merge) {
                builder.set("merge", true);
            }
            builder.set("method", method);
            if (!allowedValues.isEmpty()) {
                builder.setStructs("allowedValues", allowedValues.stream()
                        .map(CmAllowedValue::toJson)
                        .toList());
            }
            return builder.build();
        }
    }

    record CmAllowedValueImpl(String value, Optional<String> description) implements CmAllowedValue {

        CmAllowedValueImpl(Hson.Struct struct) {
            this(string(struct, "value"),
                    struct.stringValue("description"));
        }

        @Override
        public Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            builder.set("value", value);
            description.ifPresent(it -> builder.set("description", it));
            return builder.build();
        }
    }

    private static String string(Hson.Struct struct, String key) {
        return struct.stringValue(key).orElseThrow(() -> new IllegalStateException(key + " is required"));
    }

    private static <T> List<T> list(Hson.Struct struct, String key, Function<Hson.Struct, T> function) {
        return struct.structArray(key).stream()
                .flatMap(Collection::stream)
                .map(function)
                .toList();
    }

    private static List<String> list(Hson.Struct struct, String key) {
        return struct.stringArray(key).stream()
                .flatMap(Collection::stream)
                .toList();
    }
}
