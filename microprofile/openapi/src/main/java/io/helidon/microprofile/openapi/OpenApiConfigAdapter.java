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
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * {@link OpenApiConfig} adapter for {@link MpOpenApiManagerConfig}.
 */
final class OpenApiConfigAdapter implements OpenApiConfig {

    private final MpOpenApiManagerConfig config;

    OpenApiConfigAdapter(MpOpenApiManagerConfig config) {
        this.config = config;
    }

    MpOpenApiManagerConfig unwrap() {
        return config;
    }

    @Override
    public <R, T> T getConfigValue(String propertyName, Class<R> type, Function<R, T> converter, Supplier<T> defaultValue) {
        return defaultValue.get();
    }

    @Override
    public <R, T> Map<String, T> getConfigValueMap(String propertyNamePrefix, Class<R> type, Function<R, T> converter) {
        return Map.of();
    }

    @Override
    public void setAllowNakedPathParameter(Boolean allowNakedPathParameter) {
    }

    @Override
    public String modelReader() {
        return config.modelReader()
                .orElseGet(OpenApiConfig.super::modelReader);
    }

    @Override
    public String filter() {
        return config.filter()
                .orElseGet(OpenApiConfig.super::filter);
    }

    @Override
    public boolean scanDisable() {
        return config.scanDisable();
    }

    @Override
    public Set<String> scanPackages() {
        return config.scanPackages();
    }

    @Override
    public Set<String> scanClasses() {
        return config.scanClasses();
    }

    @Override
    public Set<String> scanExcludePackages() {
        return config.scanExcludePackages();
    }

    @Override
    public Set<String> scanExcludeClasses() {
        return config.scanExcludeClasses();
    }

    @Override
    public boolean scanBeanValidation() {
        return config.scanBeanValidation();
    }

    @Override
    public List<String> servers() {
        return List.of();
//        return config.servers().stream()
//                .flatMap(server -> server.value().stream())
//                .toList();
    }

    @Override
    public List<String> pathServers(String path) {
        return List.of();
//        return config.servers().stream()
//                .flatMap(server -> server.path().stream())
//                .filter(servers -> path.equals(servers.path()))
//                .flatMap(entry -> entry.servers().stream())
//                .toList();
    }

    @Override
    public List<String> operationServers(String operationId) {
        return List.of();
//        return config.servers().stream()
//                .flatMap(server -> server.operation().stream())
//                .filter(servers -> operationId.equals(servers.operation()))
//                .flatMap(entry -> entry.servers().stream())
//                .toList();
    }

    @Override
    public boolean scanDependenciesDisable() {
        return config.scanDependenciesDisable();
    }

    @Override
    public Set<String> scanDependenciesJars() {
        return config.scanDependenciesJars();
    }

    @Override
    public boolean arrayReferencesEnable() {
        return config.arrayReferencesEnable();
    }

    @Override
    public String customSchemaRegistryClass() {
        return config.customSchemaRegistryClass()
                .orElseGet(OpenApiConfig.super::customSchemaRegistryClass);
    }

    @Override
    public boolean applicationPathDisable() {
        return config.applicationPathDisable();
    }

    @Override
    public boolean privatePropertiesEnable() {
        return config.privatePropertiesEnable();
    }

    @Override
    public String propertyNamingStrategy() {
        return config.propertyNamingStrategy()
                .orElseGet(OpenApiConfig.super::propertyNamingStrategy);
    }

    @Override
    public boolean sortedPropertiesEnable() {
        return config.sortedPropertiesEnable();
    }

    @Override
    public Map<String, String> getSchemas() {
        return config.schemas();
    }

    @Override
    public String getOpenApiVersion() {
        return config.openApiVersion()
                .orElseGet(OpenApiConfig.super::getOpenApiVersion);
    }

    @Override
    public String getInfoTitle() {
        return config.infoTitle()
                .orElseGet(OpenApiConfig.super::getInfoTitle);
    }

    @Override
    public String getInfoVersion() {
        return config.infoVersion()
                .orElseGet(OpenApiConfig.super::getInfoVersion);
    }

    @Override
    public String getInfoDescription() {
        return config.infoDescription()
                .orElseGet(OpenApiConfig.super::getInfoDescription);
    }

    @Override
    public String getInfoTermsOfService() {
        return config.infoTermsOfService()
                .orElseGet(OpenApiConfig.super::getInfoTermsOfService);
    }

    @Override
    public String getInfoContactEmail() {
        return config.infoContactEmail()
                .orElseGet(OpenApiConfig.super::getInfoContactEmail);
    }

    @Override
    public String getInfoContactName() {
        return config.infoContactName()
                .orElseGet(OpenApiConfig.super::getInfoContactName);
    }

    @Override
    public String getInfoContactUrl() {
        return config.infoContactName()
                .orElseGet(OpenApiConfig.super::getInfoContactUrl);
    }

    @Override
    public String getInfoLicenseName() {
        return config.infoContactName()
                .orElseGet(OpenApiConfig.super::getInfoLicenseName);
    }

    @Override
    public String getInfoLicenseUrl() {
        return config.infoContactName()
                .orElseGet(OpenApiConfig.super::getInfoLicenseUrl);
    }

    @Override
    public OperationIdStrategy getOperationIdStrategy() {
        return config.operationIdStrategy()
                .orElseGet(OpenApiConfig.super::getOperationIdStrategy);
    }

    @Override
    public DuplicateOperationIdBehavior getDuplicateOperationIdBehavior() {
        return config.duplicateOperationIdBehavior();
    }

    @Override
    public Optional<String[]> getDefaultProduces() {
        return config.defaultProduces();
    }

    @Override
    public Optional<String[]> getDefaultConsumes() {
        return config.defaultConsumes();
    }

    @Override
    public Set<String> getScanProfiles() {
        return config.scanProfiles();
    }

    @Override
    public Set<String> getScanExcludeProfiles() {
        return config.scanExcludeProfiles();
    }

    @Override
    public Map<String, String> getScanResourceClasses() {
        return config.scanResourceClasses();
    }

    @Override
    public boolean removeUnusedSchemas() {
        return config.removeUnusedSchemas();
    }

    @Override
    public Integer getMaximumStaticFileSize() {
        return config.maximumStaticFileSize()
                .orElseGet(OpenApiConfig.super::getMaximumStaticFileSize);
    }
}
