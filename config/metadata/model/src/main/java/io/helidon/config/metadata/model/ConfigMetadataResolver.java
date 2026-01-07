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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.config.metadata.model.ConfigMetadata.CmModule;
import io.helidon.config.metadata.model.ConfigMetadata.CmOption;
import io.helidon.config.metadata.model.ConfigMetadata.CmType;
import io.helidon.config.metadata.model.ConfigMetadataImpl.CmModuleImpl;
import io.helidon.config.metadata.model.ConfigMetadataImpl.CmTypeImpl;

/**
 * Config metadata resolver.
 */
final class ConfigMetadataResolver {
    private static final System.Logger LOGGER = System.getLogger(ConfigMetadataResolver.class.getName());

    private final Map<String, CmType> types = new HashMap<>();
    private final ConfigMetadata metadata;

    ConfigMetadataResolver(ConfigMetadata metadata) {
        this.metadata = metadata;
        for (var module : metadata.modules()) {
            for (var type : module.types()) {
                types.put(type.annotatedType(), type);
            }
        }
    }

    ConfigMetadata resolve() {
        List<CmModule> resolvedModules = new ArrayList<>();
        var typeCache = new HashMap<String, CmType>();
        var providersCache = new HashMap<String, List<CmType>>();
        for (var module : metadata.modules()) {
            List<CmType> resolvedTypes = new ArrayList<>();
            for (var type : module.types()) {
                var resolvedType = resolveType(type);
                typeCache.put(type.type(), resolvedType);
                for (var provide : resolvedType.provides()) {
                    providersCache.computeIfAbsent(provide, k -> new ArrayList<>()).add(resolvedType);
                }
                resolvedTypes.add(resolvedType);
            }
            resolvedModules.add(new CmModuleImpl(module.module(), List.copyOf(resolvedTypes)));
        }
        return new ConfigMetadataImpl(List.copyOf(resolvedModules), typeCache, providersCache);
    }

    private CmType resolveType(CmType type) {
        // build the reverse hierarchy (parents first)
        var hierarchy = new ArrayList<CmType>();
        for (var e : type.inherits()) {
            var t = types.get(e);
            if (t == null) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Cannot resolve inherited type: currentType={0}, inheritedType={1}",
                        type.annotatedType(), e);
            } else {
                hierarchy.addFirst(t);
            }
        }
        hierarchy.addLast(type);

        // traverse the reverse hierarchy to override options
        var options = new HashMap<String, CmOption>();
        for (var t : hierarchy) {
            for (var e : mergeOptions(t)) {
                options.put(e.key(), e);
            }
        }

        // return a new type
        return new CmTypeImpl(
                type.type(),
                type.annotatedType(),
                List.copyOf(options.values()), // read-only copy of the resolved options
                type.description(),
                type.prefix(),
                type.standalone(),
                List.of(), // empty inherits
                type.producers(),
                type.provides());
    }

    private List<CmOption> mergeOptions(CmType type) {
        List<CmOption> merged = new ArrayList<>();
        var stack = new ArrayDeque<>(type.options());
        while (!stack.isEmpty()) {
            var option = stack.pop();
            if (option.merge()) {
                var optionType = option.type().orElse(CmOption.DEFAULT_TYPE);
                if (option.complex()) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "Cannot merge option with primitive type: enclosingType={0}, optionKey={0}, optionType={1}",
                            type.annotatedType(), option.key(), optionType);
                } else {
                    var resolvedType = types.get(optionType);
                    if (resolvedType == null) {
                        LOGGER.log(System.Logger.Level.WARNING,
                                "Cannot resolve merge option type: enclosingType={0}, optionKey={0}, optionType={1}",
                                type.annotatedType(), option.key(), option.type());
                    } else {
                        var options = resolvedType.options();
                        for (int i = options.size() - 1; i >= 0; i--) {
                            stack.push(options.get(i));
                        }
                    }
                }
            } else {
                merged.add(option);
            }
        }
        return merged;
    }
}
