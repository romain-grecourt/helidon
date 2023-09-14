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
    String customSchemaRegistryClass();

    @ConfiguredOption
    boolean applicationPathDisable();

    @ConfiguredOption
    boolean privatePropertiesEnable();

    @ConfiguredOption
    String propertyNamingStrategy();

    @ConfiguredOption
    boolean sortedPropertiesEnable();

    @ConfiguredOption
    Map<String, String> schemas();

    @ConfiguredOption
    String openApiVersion();

    @ConfiguredOption
    String infoTitle();

    @ConfiguredOption
    String infoVersion();

    @ConfiguredOption
    String infoDescription();

    @ConfiguredOption
    String infoTermsOfService();

    @ConfiguredOption
    String infoContactEmail();

    @ConfiguredOption
    String infoContactName();

    @ConfiguredOption
    String infoContactUrl();

    @ConfiguredOption
    String infoLicenseName();

    @ConfiguredOption
    String infoLicenseUrl();

    @ConfiguredOption
    OpenApiConfig.OperationIdStrategy operationIdStrategy();

    @ConfiguredOption
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
    Integer maximumStaticFileSize();
}
