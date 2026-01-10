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
    public List<CmNode> roots() {
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

            // process options
            var options = node.type().map(CmType::options).orElse(List.of());
            for (var i = options.size() - 1; i >= 0; i--) {
                var option = options.get(i);
                var optionType = option.type().flatMap(this::type);
                var optionTypeName = optionType.map(CmType::type)
                        .or(option::type)
                        .orElse(CmOption.DEFAULT_TYPE);
                var optionKey = option.key()
                        .orElseThrow(() -> new IllegalStateException(
                                "Option does not have a key: enclosingType=" + node.typeName()));
                var optionPath = node.path() + "." + optionKey;
                var optionNode = new CmNodeImpl(
                        node,
                        optionPath,
                        optionKey,
                        optionTypeName,
                        optionType.orElse(null),
                        new ArrayList<>());
                node.addChild(optionNode);

                // process provider implementations
                if (option.provider()) {
                    for (var implType : providers.getOrDefault(optionTypeName, List.of())) {
                        var implTypeName = implType.type();
                        var implKey = implType.prefix().orElseThrow(() ->
                                new IllegalStateException("Provider type does not have a prefix: " + implTypeName));
                        var implPath = optionPath + "." + implKey;
                        var implNode = new CmNodeImpl(
                                optionNode,
                                implPath,
                                implKey,
                                implTypeName,
                                implType,
                                new ArrayList<>());
                        optionNode.addChild(implNode);
                        usages.computeIfAbsent(implTypeName, k -> new ArrayList<>()).add(optionNode);
                        stack.push(implNode);
                    }
                    usages.computeIfAbsent(optionTypeName, k -> new ArrayList<>()).add(optionNode);
                } else if(optionType.isPresent()) {
                    usages.computeIfAbsent(optionTypeName, k -> new ArrayList<>()).add(optionNode);
                    stack.push(optionNode);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
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
                var key = e.key().orElseThrow(() -> new IllegalStateException(
                        "Merged option does not have a key: enclosingType=" + t.type()));
                options.put(key, e);
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
                                "Cannot resolve merge option type: " + option.type().orElse(null)));
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
