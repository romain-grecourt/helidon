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

import java.util.List;
import java.util.Optional;

import io.helidon.config.metadata.model.CmModel.CmType;

/**
 * Config metadata resolver.
 */
public interface CmResolver {

    /**
     * Create a new resolver instance.
     *
     * @param metadata metadata
     * @return CmResolver
     */
    static CmResolver create(CmModel metadata) {
        return new CmResolverImpl(metadata);
    }

    /**
     * Find config metadata type that provide the given type.
     *
     * @param typeName provider type
     * @return list of types
     */
    List<CmType> providers(String typeName);

    /**
     * Find a type by name.
     *
     * @param typeName type name
     * @return type
     */
    Optional<CmType> type(String typeName);

    /**
     * Find usages of a type.
     *
     * @param typeName type name
     * @return list of tree nodes
     */
    List<CmNode> usage(String typeName);

    /**
     * Get the tree view of the config metadata.
     *
     * @return list of root tree nodes
     */
    List<CmNode> tree();
}
