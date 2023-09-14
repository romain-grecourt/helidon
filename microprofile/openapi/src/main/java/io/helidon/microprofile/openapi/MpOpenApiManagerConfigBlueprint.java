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
import java.util.Set;

import io.helidon.builder.api.Prototype;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link MpOpenApiManager} prototype.
 */
@Prototype.Blueprint
@Configured
interface MpOpenApiManagerConfigBlueprint extends SmallRyeOpenApiConfigBlueprint {

    /**
     * If {@code true} and the {@code jakarta.ws.rs.core.Application} class returns a non-empty set, endpoints defined by
     * other resources are not included in the OpenAPI document.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @ConfiguredOption
    boolean useJaxRsSemantics();

    /**
     * Specify the set of Jandex index path.
     *
     * @return list of Jandex index path
     */
    @ConfiguredOption
    List<String> indexPaths();

    /**
     * Disable annotation scanning.
     *
     * @return {@code true} if scanning is disabled
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_DISABLE
     */
    boolean scanDisable();

    /**
     * Fully qualified name of the OASModelReader implementation.
     *
     * @return FQN of the OASModelReader implementation
     * @see org.eclipse.microprofile.openapi.OASConfig#MODEL_READER
     */
    @ConfiguredOption
    String modelReader();

    /**
     * Fully qualified name of the OASFilter implementation.
     *
     * @return FQN of the OASFilter implementation
     * @see org.eclipse.microprofile.openapi.OASConfig#FILTER
     */
    @ConfiguredOption
    String filter();

    /**
     * Specify the set of packages to scan.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_PACKAGES
     */
    @ConfiguredOption
    Set<String> scanPackages();

    /**
     * Specify the set of classes to scan.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_CLASSES
     */
    @ConfiguredOption
    Set<String> scanClasses();

    /**
     * Specify the set of packages to exclude from scans.
     *
     * @return set of packages
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_EXCLUDE_PACKAGES
     */
    @ConfiguredOption
    Set<String> scanExcludePackages();

    /**
     * Specify the set of classes to exclude from scans.
     *
     * @return set of classes
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_EXCLUDE_CLASSES
     */
    @ConfiguredOption
    Set<String> scanExcludeClasses();

    /**
     * Enable or disable scanning Jakarta Bean Validation annotations.
     *
     * @return {@code true} if scanning is enabled
     * @see org.eclipse.microprofile.openapi.OASConfig#SCAN_BEANVALIDATION
     */
    @ConfiguredOption
    boolean scanBeanValidation();

    /**
     * Specify the list of global servers that provide connectivity information.
     *
     * @return list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS
     */
    @ConfiguredOption
    List<String> servers();

    /**
     * Specify alternative list of servers to service all operations in a path.
     *
     * @return Map of path to alternative list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS_PATH_PREFIX
     */
    @ConfiguredOption(key = "servers.path")
    List<AlternatePathServers> pathServers();

    /**
     * Specify an alternative list of servers to service an operation.
     *
     * @return Map of operation to alternative list of servers
     * @see org.eclipse.microprofile.openapi.OASConfig#SERVERS_OPERATION_PREFIX
     */
    @ConfiguredOption(key = "servers.operations")
    List<AlternateOperationServers> operationServers();

    /**
     * Alternate path servers.
     *
     * @param path    path
     * @param servers servers
     */
    record AlternatePathServers(String path, List<String> servers) {
    }

    /**
     * Alternate operation servers.
     *
     * @param operation operation
     * @param servers   servers
     */
    record AlternateOperationServers(String operation, List<String> servers) {
    }
}
