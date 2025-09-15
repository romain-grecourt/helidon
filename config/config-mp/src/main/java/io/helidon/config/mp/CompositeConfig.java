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
package io.helidon.config.mp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.helidon.common.GenericType;
import io.helidon.config.ConfigValue;
import io.helidon.config.spi.ConfigMapper;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Composite config that implements both MicroProfile config and Helidon config.
 */
abstract class CompositeConfig implements io.helidon.config.Config, Config {

    abstract Config mpConfig();

    abstract io.helidon.config.Config seConfig();

    @Override
    public org.eclipse.microprofile.config.ConfigValue getConfigValue(String s) {
        return mpConfig().getConfigValue(s);
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> aClass) {
        return mpConfig().getConverter(aClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> aClass) {
        if (Config.class.equals(aClass)) {
            return (T) mpConfig();
        }
        return mpConfig().unwrap(aClass);
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        return mpConfig().getValue(propertyName, propertyType);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return mpConfig().getOptionalValue(propertyName, propertyType);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return mpConfig().getPropertyNames();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return mpConfig().getConfigSources();
    }

    @Override
    public Instant timestamp() {
        return seConfig().timestamp();
    }

    @Override
    public Key key() {
        return seConfig().key();
    }

    @Override
    public io.helidon.config.Config root() {
        return seConfig().root();
    }

    @Override
    public io.helidon.config.Config get(Key key) {
        return seConfig().get(key);
    }

    @Override
    public io.helidon.config.Config detach() {
        return seConfig().detach();
    }

    @Override
    public Type type() {
        return seConfig().type();
    }

    @Override
    public boolean hasValue() {
        return seConfig().hasValue();
    }

    @Override
    public Stream<io.helidon.config.Config> traverse(Predicate<io.helidon.config.Config> predicate) {
        return seConfig().traverse();
    }

    @Override
    public <T> T convert(Class<T> type, String value) {
        return seConfig().convert(type, value);
    }

    @Override
    public <T> ConfigValue<T> as(GenericType<T> genericType) {
        return seConfig().as(genericType);
    }

    @Override
    public <T> ConfigValue<T> as(Class<T> type) {
        return seConfig().as(type);
    }

    @Override
    public <T> ConfigValue<T> as(Function<io.helidon.config.Config, T> mapper) {
        return seConfig().as(mapper);
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Class<T> type) {
        return seConfig().asList(type);
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Function<io.helidon.config.Config, T> mapper) {
        return seConfig().asList(mapper);
    }

    @Override
    public ConfigValue<List<io.helidon.config.Config>> asNodeList() {
        return seConfig().asNodeList();
    }

    @Override
    public ConfigValue<Map<String, String>> asMap() {
        return seConfig().asMap();
    }

    @Override
    public ConfigMapper mapper() {
        return seConfig().mapper();
    }

    static final class MpDelegate extends CompositeConfig {

        private final AtomicReference<Config> mpConfigRef = new AtomicReference<>();
        private final AtomicReference<io.helidon.config.Config> seConfigRef = new AtomicReference<>();

        MpDelegate(Config mpConfig) {
            set(mpConfig);
        }

        void set(Config mpConfig) {
            mpConfigRef.set(mpConfig);
            if (mpConfig instanceof io.helidon.config.Config seConfig) {
                seConfigRef.set(seConfig);
            } else {
                seConfigRef.set(MpConfig.toHelidonConfig(mpConfig));
            }
        }

        @Override
        Config mpConfig() {
            return mpConfigRef.get();
        }

        @Override
        io.helidon.config.Config seConfig() {
            return seConfigRef.get();
        }
    }

    static final class SeDelegate extends CompositeConfig {

        private final Config mpConfig;
        private final io.helidon.config.Config seConfig;

        SeDelegate(io.helidon.config.Config seConfig) {
            this.mpConfig = toMpConfig(seConfig);
            this.seConfig = seConfig;
        }

        @Override
        Config mpConfig() {
            return mpConfig;
        }

        @Override
        io.helidon.config.Config seConfig() {
            return seConfig;
        }

        static Config toMpConfig(io.helidon.config.Config config) {
            return new MpConfigBuilder()
                    .withSources(MpConfigSources.create(config))
                    .build();
        }
    }
}
