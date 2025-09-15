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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.common.GenericType;
import io.helidon.config.spi.ConfigMapper;

/**
 * Config delegate backed a by supplier.
 */
record ConfigDelegate(Supplier<Config> delegate) implements Config {

    @Override
    public Context context() {
        return delegate.get().context();
    }

    @Override
    public Instant timestamp() {
        return delegate.get().timestamp();
    }

    @Override
    public Key key() {
        return delegate.get().key();
    }

    @Override
    public String name() {
        return delegate.get().name();
    }

    @Override
    public Config get(String key) {
        return delegate.get().get(key);
    }

    @Override
    public Config root() {
        return delegate.get().root();
    }

    @Override
    public Config get(Key key) {
        return delegate.get().get(key);
    }

    @Override
    public Config detach() {
        return delegate.get().detach();
    }

    @Override
    public Type type() {
        return delegate.get().type();
    }

    @Override
    public boolean exists() {
        return delegate.get().exists();
    }

    @Override
    public boolean isLeaf() {
        return delegate.get().isLeaf();
    }

    @Override
    public boolean isObject() {
        return delegate.get().isObject();
    }

    @Override
    public boolean isList() {
        return delegate.get().isList();
    }

    @Override
    public boolean hasValue() {
        return delegate.get().hasValue();
    }

    @Override
    public void ifExists(Consumer<Config> action) {
        delegate.get().ifExists(action);
    }

    @Override
    public Stream<Config> traverse() {
        return delegate.get().traverse();
    }

    @Override
    public Stream<Config> traverse(Predicate<Config> predicate) {
        return traverse().filter(predicate);
    }

    @Override
    public <T> T convert(Class<T> type, String value) throws ConfigMappingException {
        return delegate.get().convert(type, value);
    }

    @Override
    public ConfigMapper mapper() {
        return delegate.get().mapper();
    }

    @Override
    public <T> ConfigValue<T> as(GenericType<T> genericType) {
        return delegate.get().as(genericType);
    }

    @Override
    public <T> ConfigValue<T> as(Class<T> type) {
        return delegate.get().as(type);
    }

    @Override
    public <T> ConfigValue<T> as(Function<Config, T> mapper) {
        return delegate.get().as(mapper);
    }

    @Override
    public ConfigValue<Boolean> asBoolean() {
        return delegate.get().asBoolean();
    }

    @Override
    public ConfigValue<String> asString() {
        return delegate.get().asString();
    }

    @Override
    public ConfigValue<Integer> asInt() {
        return delegate.get().asInt();
    }

    @Override
    public ConfigValue<Long> asLong() {
        return delegate.get().asLong();
    }

    @Override
    public ConfigValue<Double> asDouble() {
        return delegate.get().asDouble();
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Class<T> type) throws ConfigMappingException {
        return delegate.get().asList(type);
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Function<Config, T> mapper) throws ConfigMappingException {
        return delegate.get().asList(mapper);
    }

    @Override
    public ConfigValue<Config> asNode() {
        return delegate.get().asNode();
    }

    @Override
    public ConfigValue<List<Config>> asNodeList() throws ConfigMappingException {
        return delegate.get().asNodeList();
    }

    @Override
    public ConfigValue<Map<String, String>> asMap() throws MissingValueException {
        return delegate.get().asMap();
    }

    @Override
    public void onChange(Consumer<Config> onChangeConsumer) {
        delegate.get().onChange(onChangeConsumer);
    }
}
