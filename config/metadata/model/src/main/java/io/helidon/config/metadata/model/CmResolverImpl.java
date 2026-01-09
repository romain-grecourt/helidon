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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.config.metadata.model.CmModel.CmOption;
import io.helidon.config.metadata.model.CmModel.CmType;
import io.helidon.config.metadata.model.CmModelImpl.CmTypeImpl;

/**
 * Config metadata resolver.
 */
final class CmResolverImpl implements CmResolver {

    private final CmModel metadata;
    private final Map<String, CmType> prefixes = new HashMap<>(); // standalone types by prefixes
    private final Map<String, CmType> types = new HashMap<>(); // unresolved types
    private final Map<String, CmType> resolvedTypes = new HashMap<>(); // resolved types
    private final Map<String, List<CmType>> providers = new HashMap<>(); // providers by contract
    private final Map<String, List<CmNode>> usages = new HashMap<>(); // tree nodes by option type
    private final List<CmNode> tree = new ArrayList<>();
    private final List<CmNode> readOnlyTree = Collections.unmodifiableList(tree);

    CmResolverImpl(CmModel metadata) {
        this.metadata = metadata;
        init();
        resolveTypes();
        initTree();
    }

    @Override
    public Optional<CmType> type(String typeName) {
        return Optional.ofNullable(resolvedTypes.get(typeName));
    }

    @Override
    public List<CmType> providers(String typeName) {
        return Collections.unmodifiableList(providers.getOrDefault(typeName, List.of()));
    }

    @Override
    public List<String> contracts() {
        return List.copyOf(providers.keySet());
    }

    @Override
    public List<CmNode> usage(String typeName) {
        return Collections.unmodifiableList(usages.getOrDefault(typeName, List.of()));
    }

    @Override
    public List<CmNode> tree() {
        return readOnlyTree;
    }

    private void init() {
        for (var module : metadata.modules()) {
            for (var type : module.types()) {
                types.put(type.type(), type);
                for (var provide : type.provides()) {
                    providers.computeIfAbsent(provide, k -> new ArrayList<>()).add(type);
                }
                if (type.standalone()) {
                    var prefix = type.prefix()
                            .orElseThrow(() -> new IllegalStateException(
                                    "Standalone type does not have a prefix: " + type.type()));
                    prefixes.put(prefix, type);
                }
            }
        }
    }

    private void resolveTypes() {
        types.forEach((k, v) -> resolvedTypes.put(k, resolveType(v)));
    }

    private void initTree() {
        var stack = new ArrayDeque<CmNodeImpl>();
        for (var entry : prefixes.entrySet()) {
            var prefix = entry.getKey();
            var type = entry.getValue();
            var segments = prefix.split("\\.");
            if (segments.length > 1 && prefixes.containsKey(segments[0])) {
                // not a true root
                continue;
            }

            var typeName = type.type();
            var resolvedType = resolvedTypes.get(typeName);
            var key = segments[segments.length - 1];
            var node = new CmNodeImpl(null, prefix, key, typeName, resolvedType, new ArrayList<>());
            tree.add(node);
            stack.push(node);
        }

        // depth-first traversal
        while (!stack.isEmpty()) {
            var node = stack.pop();
            var options = node.type().map(CmType::options).orElse(List.of());
            for (var i = options.size() - 1; i >= 0; i--) {
                var option = options.get(i);
                var resolvedType = option.type().flatMap(this::type);
                var resolvedTypeName = resolvedType.map(CmType::type).orElse(CmOption.DEFAULT_TYPE);
                var path = node.path() + "." + option.key();
                var child = new CmNodeImpl(
                        node,
                        path,
                        option.key(),
                        resolvedTypeName,
                        resolvedType.orElse(null),
                        new ArrayList<>());
                node.addChild(child);
                if (resolvedType.isPresent()) {
                    usages.computeIfAbsent(resolvedTypeName, k -> new ArrayList<>()).add(child);
                    stack.push(child);
                }
            }
        }
    }

    private CmType resolveType(CmType type) {
        // build the reverse hierarchy (parents first)
        var hierarchy = new ArrayList<CmType>();
        for (var e : type.inherits()) {
            var t = types.get(e);
            if (t == null) {
                throw new IllegalStateException(
                        "Cannot resolve inherited type: currentType=%s, inheritedType=%s"
                                .formatted(type.type(), e));
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

        // return a copy with updates
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
                var resolvedType = option.type()
                        .flatMap(it -> Optional.ofNullable(types.get(it)))
                        .orElseThrow(() -> new IllegalStateException(
                                "Cannot resolve merge option type: optionKey=%s, optionType=%s"
                                        .formatted(option.key(), option.type().orElse(null))));
                var options = resolvedType.options();
                for (int i = options.size() - 1; i >= 0; i--) {
                    stack.push(options.get(i));
                }
            } else {
                merged.add(option);
            }
        }
        return merged;
    }
}
