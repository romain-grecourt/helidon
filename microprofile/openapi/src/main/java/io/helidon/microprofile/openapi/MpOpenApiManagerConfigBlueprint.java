/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.microprofile.openapi;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Prototype;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link MpOpenApiManager} prototype.
 */
@Prototype.Blueprint
interface MpOpenApiManagerConfigBlueprint extends SmallRyeOpenApiConfigBlueprint {

    /**
     * If {@code true} and the {@code jakarta.ws.rs.core.Application} class returns a non-empty set, endpoints defined by
     * other resources are not included in the OpenAPI document.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @ConfiguredOption(configured = false)
    boolean useJaxRsSemantics();

    /**
     * Specify the set of Jandex index path.
     *
     * @return list of Jandex index path
     */
    @ConfiguredOption(configured = false, value = "META-INF/jandex.idx")
    List<String> indexPaths();

    /**
     * Disable annotation scanning.
     *
     * @return {@code true} if scanning is disabled
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_DISABLE
     */
    @ConfiguredOption(configured = false,key = "scan.disable")
    boolean scanDisable();

    /**
     * Fully qualified name of the OASModelReader implementation.
     *
     * @return FQN of the OASModelReader implementation
     * @see org.eclipse.microprofile.openapi.OASConfig#MODEL_READER
     */
    @ConfiguredOption(configured = false,key = "model.reader")
    Optional<String> modelReader();

    /**
     * Fully qualified name of the OASFilter implementation.
     *
     * @return FQN of the OASFilter implementation
     * @see org.eclipse.microprofile.openapi.OASConfig#FILTER
     */
    @ConfiguredOption(configured = false)
    Optional<String> filter();

    /**
     * Specify the set of packages to scan.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_PACKAGES
     */
    @ConfiguredOption(configured = false, key = "scan.packages")
    Set<String> scanPackages();

    /**
     * Specify the set of classes to scan.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_CLASSES
     */
    @ConfiguredOption(configured = false, key = "scan.classes")
    Set<String> scanClasses();

    /**
     * Specify the set of packages to exclude from scans.
     *
     * @return set of packages
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_EXCLUDE_PACKAGES
     */
    @ConfiguredOption(configured = false, key = "scan.exclude.packages")
    Set<String> scanExcludePackages();

    /**
     * Specify the set of classes to exclude from scans.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_EXCLUDE_CLASSES
     */
    @ConfiguredOption(configured = false, key = "scan.exclude.classes")
    Set<String> scanExcludeClasses();

    /**
     * Enable or disable scanning Jakarta Bean Validation annotations.
     *
     * @return {@code true} if scanning is enabled
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_BEANVALIDATION
     */
    @ConfiguredOption(configured = false, key = "scan.beanvalidation")
    boolean scanBeanValidation();

    /**
     * Specify the list of global servers that provide connectivity information.
     *
     * @return list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS
     */
    @ConfiguredOption(configured = false, key = "servers")
    @Prototype.Singular
    List<String> servers();

    /**
     * Specify alternative list of servers to service all operations in a path.
     *
     * @return Map of path to alternative list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS_PATH_PREFIX
     */
    @ConfiguredOption(configured = false, key = "servers.path")
    @Prototype.Singular
    Map<String, String> pathServers();

    /**
     * Specify an alternative list of servers to service an operation.
     *
     * @return Map of operation to alternative list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS_OPERATION_PREFIX
     */
    @ConfiguredOption(configured = false, key = "servers.operation")
    @Prototype.Singular
    Map<String, String> operationServers();
}
