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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Prototype;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy;

/**
 * Configuration prototype that mirrors {@link io.smallrye.openapi.api.OpenApiConfig}.
 */
@Prototype.Blueprint
@Configured
interface SmallRyeOpenApiConfigBlueprint {

    @ConfiguredOption
    boolean scanDependenciesDisable();

    @ConfiguredOption
    Set<String> scanDependenciesJars();

    @ConfiguredOption
    boolean arrayReferencesEnable();

    @ConfiguredOption
    Optional<String> customSchemaRegistryClass();

    @ConfiguredOption
    boolean applicationPathDisable();

    @ConfiguredOption
    boolean privatePropertiesEnable();

    @ConfiguredOption
    Optional<String> propertyNamingStrategy();

    @ConfiguredOption
    boolean sortedPropertiesEnable();

    @ConfiguredOption(key = "schema")
    @Prototype.Singular
    Map<String, String> schemas();

    @ConfiguredOption
    Optional<String> openApiVersion();

    @ConfiguredOption
    Optional<String> infoTitle();

    @ConfiguredOption
    Optional<String> infoVersion();

    @ConfiguredOption
    Optional<String> infoDescription();

    @ConfiguredOption
    Optional<String> infoTermsOfService();

    @ConfiguredOption
    Optional<String> infoContactEmail();

    @ConfiguredOption
    Optional<String> infoContactName();

    @ConfiguredOption
    Optional<String> infoContactUrl();

    @ConfiguredOption
    Optional<String> infoLicenseName();

    @ConfiguredOption
    Optional<String> infoLicenseUrl();

    @ConfiguredOption
    Optional<OperationIdStrategy> operationIdStrategy();

    @ConfiguredOption(value = "WARN")
    OpenApiConfig.DuplicateOperationIdBehavior duplicateOperationIdBehavior();

    @ConfiguredOption
    Optional<String[]> defaultProduces();

    @ConfiguredOption
    Optional<String[]> defaultConsumes();

    @ConfiguredOption
    Set<String> scanProfiles();

    @ConfiguredOption
    Set<String> scanExcludeProfiles();

    @ConfiguredOption
    Map<String, String> scanResourceClasses();

    @ConfiguredOption
    boolean removeUnusedSchemas();

    @ConfiguredOption
    Optional<Integer> maximumStaticFileSize();
}
