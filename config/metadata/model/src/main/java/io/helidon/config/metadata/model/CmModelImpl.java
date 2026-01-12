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
package io.helidon.config.metadata.model;

import java.util.List;
import java.util.Optional;

import io.helidon.metadata.hson.Hson;
import io.helidon.metadata.hson.HsonNotFoundException;

record CmModelImpl(List<CmModule> modules) implements CmModel {

    CmModelImpl(Hson.Array array) {
        this(array.getStructs().stream().map(CmModule::fromJson).toList());
    }

    @Override
    public Hson.Array toJson() {
        return Hson.Array.create(modules.stream()
                .map(CmModule::toJson)
                .toList());
    }

    record CmModuleImpl(String module, List<CmType> types) implements CmModule {

        CmModuleImpl(Hson.Struct struct) {
            this(struct.stringValue("module", HsonNotFoundException::new),
                    struct.structArray("types", CmType::fromJson).orElseGet(List::of));
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
                      List<CmOption> options,
                      Optional<String> description,
                      Optional<String> prefix,
                      boolean standalone,
                      List<String> inherits,
                      List<String> producers,
                      List<String> provides) implements CmType {

        CmTypeImpl {
            if (standalone && prefix.isEmpty()) {
                throw new IllegalArgumentException("Standalone type does not have a prefix: " + type);
            }
            if (!provides.isEmpty()) {
                if (standalone) {
                    throw new IllegalArgumentException("Standalone type cannot implement contract(s): " + type);
                }
                if (prefix.isEmpty()) {
                    throw new IllegalArgumentException("Provider implementation does not have a prefix: " + type);
                }
            }
        }

        CmTypeImpl(Hson.Struct struct) {
            this(struct.stringValue("type", HsonNotFoundException::new),
                    struct.structArray("options", CmOption::fromJson).orElseGet(List::of),
                    struct.stringValue("description"),
                    struct.stringValue("prefix"),
                    struct.booleanValue("standalone").orElse(false),
                    struct.stringArray("inherits").orElseGet(List::of),
                    struct.stringArray("producers").orElseGet(List::of),
                    struct.stringArray("provides").orElseGet(List::of));
        }

        @Override
        public int compareTo(CmType o) {
            return type.compareTo(o.type());
        }

        @Override
        public Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            builder.set("type", type);
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

    record CmOptionImpl(Optional<String> key,
                        Optional<String> description,
                        String type,
                        Optional<String> defaultValue,
                        boolean required,
                        boolean experimental,
                        boolean deprecated,
                        boolean provider,
                        boolean merge,
                        Kind kind,
                        List<CmAllowedValue> allowedValues) implements CmOption {

        CmOptionImpl {
            if (required && defaultValue.isPresent()) {
                throw new IllegalArgumentException(
                        "Required option cannot have a default value: key=%s, type=%s"
                                .formatted(key, type));
            }
        }

        CmOptionImpl(Hson.Struct struct) {
            this(struct.stringValue("key"),
                    struct.stringValue("description"),
                    struct.stringValue("type").orElse(DEFAULT_TYPE),
                    struct.stringValue("defaultValue"),
                    struct.booleanValue("required").orElse(false),
                    struct.booleanValue("experimental").orElse(false),
                    struct.booleanValue("deprecated").orElse(false),
                    struct.booleanValue("provider").orElse(false),
                    struct.booleanValue("merge").orElse(false),
                    struct.stringValue("kind").map(Kind::valueOf).orElse(DEFAULT_KIND),
                    struct.structArray("allowedValues", CmAllowedValue::fromJson).orElseGet(List::of));
        }

        @Override
        public int compareTo(CmOption o) {
            var thisKey = key.orElse(null);
            var thatKey = o.key().orElse(null);
            if (thisKey != null) {
                if (thatKey != null) {
                    return thisKey.compareTo(thatKey);
                }
                return -1;
            }
            return 1;
        }

        @Override
        public Hson.Struct toJson() {
            var builder = Hson.Struct.builder();
            key.ifPresent(it -> builder.set("key", it));
            if (!type.equals(DEFAULT_TYPE)) {
                builder.set("type", type);
            }
            description.ifPresent(it -> builder.set("description", it));
            defaultValue.ifPresent(it -> builder.set("defaultValue", it));
            if (experimental) {
                builder.set("experimental", true);
            }
            if (required) {
                builder.set("required", true);
            }
            if (!kind.equals(DEFAULT_KIND)) {
                builder.set("kind", kind.name());
            }
            if (provider) {
                builder.set("provider", true);
            }
            if (deprecated) {
                builder.set("deprecated", true);
            }
            if (merge) {
                builder.set("merge", true);
            }
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
            this(struct.stringValue("value", HsonNotFoundException::new),
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
}
