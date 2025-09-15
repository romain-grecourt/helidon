/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.config;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.helidon.common.GenericType;
import io.helidon.config.spi.ConfigMapper;

/**
 * Common config adapter.
 */
@SuppressWarnings("removal")
class CommonConfig implements Config {

    private final Config emptyConfig;
    private final Instant timestamp;
    private final io.helidon.common.config.Config delegate;

    CommonConfig(io.helidon.common.config.Config delegate) {
        this(Config.empty(), Instant.now(), delegate);
    }

    private CommonConfig(Config realConfig, Instant timestamp, io.helidon.common.config.Config delegate) {
        this.emptyConfig = realConfig;
        this.delegate = delegate;
        this.timestamp = timestamp;
    }

    static Key wrap(io.helidon.common.config.Config.Key key) {
        if (key instanceof Key hk) {
            return hk;
        }
        return new CommonConfig.KeyWrapper(key);
    }

    static Config wrap(io.helidon.common.config.Config config) {
        if (config instanceof Config cfg) {
            return cfg;
        }
        return new CommonConfig(config);
    }

    io.helidon.common.config.Config delegate() {
        return delegate;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public Key key() {
        return new KeyWrapper(delegate.key());
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Config get(String key) {
        return new CommonConfig(emptyConfig, timestamp, delegate.get(key));
    }

    @Override
    public Config root() {
        return new CommonConfig(emptyConfig, timestamp, delegate.root());
    }

    @Override
    public Config get(Key key) {
        return new CommonConfig(emptyConfig, timestamp, delegate.get(key));
    }

    @Override
    public Config detach() {
        return new CommonConfig(emptyConfig, timestamp, delegate.detach());
    }

    @Override
    public Type type() {
        if (delegate.isList()) {
            return Type.LIST;
        }
        if (delegate.isObject()) {
            return Type.OBJECT;
        }
        if (delegate.exists()) {
            return Type.VALUE;
        }
        return Type.MISSING;
    }

    @Override
    public boolean exists() {
        return delegate.exists();
    }

    @Override
    public boolean isLeaf() {
        return delegate.isLeaf();
    }

    @Override
    public boolean isObject() {
        return delegate.isObject();
    }

    @Override
    public boolean isList() {
        return delegate.isList();
    }

    @Override
    public boolean hasValue() {
        return delegate.hasValue();
    }

    @Override
    public void ifExists(Consumer<Config> action) {
        if (delegate.exists()) {
            action.accept(this);
        }
    }

    @Override
    public Stream<Config> traverse() {
        return delegate.asList(io.helidon.common.config.Config.class).stream()
                .flatMap(List::stream)
                .map(it -> new CommonConfig(emptyConfig, timestamp, it));
    }

    @Override
    public Stream<Config> traverse(Predicate<Config> predicate) {
        return traverse()
                .filter(predicate);
    }

    @Override
    public <T> T convert(Class<T> type, String value) throws ConfigMappingException {
        return emptyConfig.convert(type, value);
    }

    @Override
    public ConfigMapper mapper() {
        return emptyConfig.mapper();
    }

    @Override
    public ConfigValue<String> asString() {
        var commonValue = delegate.asString();
        if (commonValue.isPresent()) {
            return ConfigValues.create(this, commonValue::asOptional, Config::asString);
        }
        return ConfigValues.create(this, Optional::empty, Config::asString);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConfigValue<T> as(GenericType<T> genericType) {
        if (genericType.isClass()) {
            return (ConfigValue<T>) as(genericType.rawType());
        }
        return ConfigValues.create(this, genericType, mapper());
    }

    @Override
    public <T> ConfigValue<T> as(Class<T> type) {
        return ConfigValues.create(this, type, mapper());
    }

    @Override
    public <T> ConfigValue<T> as(Function<Config, T> mapper) {
        return ConfigValues.create(this, mapper);
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Class<T> type) throws ConfigMappingException {
        return ConfigValues.createList(this, cfg -> cfg.as(type), cfg -> cfg.asList(type));
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Function<Config, T> mapper) throws ConfigMappingException {
        return ConfigValues.createList(this, cfg -> cfg.as(mapper), cfg -> cfg.asList(mapper));
    }

    @Override
    public ConfigValue<List<Config>> asNodeList() throws ConfigMappingException {
        var nodeList = delegate.asNodeList();
        if (nodeList.isEmpty()) {
            return ConfigValues.create(this, Optional::empty, Config::asNodeList);
        }
        return ConfigValues.create(this, () -> Optional.of(asNodeList(nodeList)), Config::asNodeList);
    }

    @Override
    public ConfigValue<Map<String, String>> asMap() throws MissingValueException {
        return ConfigValues.createMap(this, mapper());
    }

    @Override
    public io.helidon.common.config.Config get(io.helidon.common.config.Config.Key key) {
        return delegate.get(key);
    }

    @Override
    @SuppressWarnings("ALL")
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    private static List<Config> asNodeList(io.helidon.common.config.ConfigValue<List<io.helidon.common.config.Config>> nodeList) {
        return nodeList.stream()
                .flatMap(List::stream)
                .map(CommonConfig::wrap)
                .toList();
    }

    @SuppressWarnings("removal")
    private static class KeyWrapper implements Key {
        private final io.helidon.common.config.Config.Key delegate;

        KeyWrapper(io.helidon.common.config.Config.Key key) {
            this.delegate = key;
        }

        @Override
        public Key parent() {
            return new KeyWrapper(delegate.parent());
        }

        @Override
        public Key child(io.helidon.common.config.Config.Key key) {
            return new KeyWrapper(delegate.child(key));
        }

        @Override
        public boolean isRoot() {
            return delegate.isRoot();
        }

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        @SuppressWarnings("ALL")
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public int compareTo(io.helidon.common.config.Config.Key o) {
            return delegate.compareTo(o);
        }
    }
}
