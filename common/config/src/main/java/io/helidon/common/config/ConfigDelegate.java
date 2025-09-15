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
package io.helidon.common.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Config delegate backed by a supplier.
 */
@SuppressWarnings("removal")
final class ConfigDelegate implements Config {

    private final Supplier<Config> delegate;

    ConfigDelegate(Supplier<Config> delegate) {
        this.delegate = delegate;
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
    public Config get(String key) throws ConfigException {
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
    public Config detach() throws ConfigException {
        return delegate.get().detach();
    }

    @Override
    public boolean exists() {
        return delegate.get().exists();
    }

    @Override
    public Stream<? extends Config> traverse() {
        return delegate.get().traverse();
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
    public <T> ConfigValue<T> as(Class<T> type) {
        return delegate.get().as(type);
    }

    @Override
    public <T> ConfigValue<T> map(Function<Config, T> mapper) {
        return delegate.get().map(mapper);
    }

    @Override
    public <T> ConfigValue<List<T>> asList(Class<T> type) throws ConfigException {
        return delegate.get().asList(type);
    }

    @Override
    public <T> ConfigValue<List<T>> mapList(Function<Config, T> mapper) throws ConfigException {
        return delegate.get().mapList(mapper);
    }

    @Override
    public <C extends Config> ConfigValue<List<C>> asNodeList() throws ConfigException {
        return delegate.get().asNodeList();
    }

    @Override
    public ConfigValue<Map<String, String>> asMap() throws ConfigException {
        return delegate.get().asMap();
    }

    @Override
    public ConfigValue<? extends Config> asNode() {
        return delegate.get().asNode();
    }

    @Override
    public ConfigValue<String> asString() {
        return delegate.get().asString();
    }

    @Override
    public ConfigValue<Boolean> asBoolean() {
        return delegate.get().asBoolean();
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
}
