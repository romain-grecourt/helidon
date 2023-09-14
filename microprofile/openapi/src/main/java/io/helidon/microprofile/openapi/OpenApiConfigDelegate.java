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
 * {@link OpenApiConfig} delegate.
 */
class OpenApiConfigDelegate implements OpenApiConfig {

    private final OpenApiConfig delegate;

    OpenApiConfigDelegate(OpenApiConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public <R, T> T getConfigValue(String propertyName, Class<R> type, Function<R, T> converter, Supplier<T> defaultValue) {
        return delegate.getConfigValue(propertyName, type, converter, defaultValue);
    }

    @Override
    public <R, T> Map<String, T> getConfigValueMap(String propertyNamePrefix, Class<R> type, Function<R, T> converter) {
        return delegate.getConfigValueMap(propertyNamePrefix, type, converter);
    }

    @Override
    public <T> T getConfigValue(String propertyName, Class<T> type, Supplier<T> defaultValue) {
        return delegate.getConfigValue(propertyName, type, defaultValue);
    }

    @Override
    public String modelReader() {
        return delegate.modelReader();
    }

    @Override
    public String filter() {
        return delegate.filter();
    }

    @Override
    public boolean scanDisable() {
        return delegate.scanDisable();
    }

    @Override
    public Set<String> scanPackages() {
        return delegate.scanPackages();
    }

    @Override
    public Set<String> scanClasses() {
        return delegate.scanClasses();
    }

    @Override
    public Set<String> scanExcludePackages() {
        return delegate.scanExcludePackages();
    }

    @Override
    public Set<String> scanExcludeClasses() {
        return delegate.scanExcludeClasses();
    }

    @Override
    public boolean scanBeanValidation() {
        return delegate.scanBeanValidation();
    }

    @Override
    public List<String> servers() {
        return delegate.servers();
    }

    @Override
    public List<String> pathServers(String path) {
        return delegate.pathServers(path);
    }

    @Override
    public List<String> operationServers(String operationId) {
        return delegate.operationServers(operationId);
    }

    @Override
    public boolean scanDependenciesDisable() {
        return delegate.scanDependenciesDisable();
    }

    @Override
    public Set<String> scanDependenciesJars() {
        return delegate.scanDependenciesJars();
    }

    @Override
    public boolean arrayReferencesEnable() {
        return delegate.arrayReferencesEnable();
    }

    @Override
    public String customSchemaRegistryClass() {
        return delegate.customSchemaRegistryClass();
    }

    @Override
    public boolean applicationPathDisable() {
        return delegate.applicationPathDisable();
    }

    @Override
    public boolean privatePropertiesEnable() {
        return delegate.privatePropertiesEnable();
    }

    @Override
    public String propertyNamingStrategy() {
        return delegate.propertyNamingStrategy();
    }

    @Override
    public boolean sortedPropertiesEnable() {
        return delegate.sortedPropertiesEnable();
    }

    @Override
    public Map<String, String> getSchemas() {
        return delegate.getSchemas();
    }

    @Override
    public String getOpenApiVersion() {
        return delegate.getOpenApiVersion();
    }

    @Override
    public String getInfoTitle() {
        return delegate.getInfoTitle();
    }

    @Override
    public String getInfoVersion() {
        return delegate.getInfoVersion();
    }

    @Override
    public String getInfoDescription() {
        return delegate.getInfoDescription();
    }

    @Override
    public String getInfoTermsOfService() {
        return delegate.getInfoTermsOfService();
    }

    @Override
    public String getInfoContactEmail() {
        return delegate.getInfoContactEmail();
    }

    @Override
    public String getInfoContactName() {
        return delegate.getInfoContactName();
    }

    @Override
    public String getInfoContactUrl() {
        return delegate.getInfoContactUrl();
    }

    @Override
    public String getInfoLicenseName() {
        return delegate.getInfoLicenseName();
    }

    @Override
    public String getInfoLicenseUrl() {
        return delegate.getInfoLicenseUrl();
    }

    @Override
    public OperationIdStrategy getOperationIdStrategy() {
        return delegate.getOperationIdStrategy();
    }

    @Override
    public DuplicateOperationIdBehavior getDuplicateOperationIdBehavior() {
        return delegate.getDuplicateOperationIdBehavior();
    }

    @Override
    public Optional<String[]> getDefaultProduces() {
        return delegate.getDefaultProduces();
    }

    @Override
    public Optional<String[]> getDefaultConsumes() {
        return delegate.getDefaultConsumes();
    }

    @Override
    public Optional<Boolean> allowNakedPathParameter() {
        return delegate.allowNakedPathParameter();
    }

    @Override
    public void setAllowNakedPathParameter(Boolean allowNakedPathParameter) {
        delegate.setAllowNakedPathParameter(allowNakedPathParameter);
    }

    @Override
    public void doAllowNakedPathParameter() {
        delegate.doAllowNakedPathParameter();
    }

    @Override
    public Set<String> getScanProfiles() {
        return delegate.getScanProfiles();
    }

    @Override
    public Set<String> getScanExcludeProfiles() {
        return delegate.getScanExcludeProfiles();
    }

    @Override
    public Map<String, String> getScanResourceClasses() {
        return delegate.getScanResourceClasses();
    }

    @Override
    public boolean removeUnusedSchemas() {
        return delegate.removeUnusedSchemas();
    }

    @Override
    public Integer getMaximumStaticFileSize() {
        return delegate.getMaximumStaticFileSize();
    }

    @Override
    public Set<String> toSet(String[] items) {
        return delegate.toSet(items);
    }

    @Override
    public List<String> toList(String[] items) {
        return delegate.toList(items);
    }
}
