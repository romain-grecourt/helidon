/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import io.helidon.common.DeprecationSupport;

/**
 * Providers that can be loaded from configuration should implement this interface.
 *
 * @param <T> type of the service this provider provides
 */
@SuppressWarnings({"removal"})
public interface ConfiguredProvider<T extends NamedService> extends io.helidon.common.config.ConfiguredProvider<T> {

    /**
     * Key this service implementation is stored under. This is also considered the service "type" when used
     * in a list in configuration, to allow the same service defined more than once.
     *
     * @return key of this implementation
     */
    @Override
    String configKey();

    @Override
    @SuppressWarnings("removal")
    default T create(io.helidon.common.config.Config config, String name) {
        return create(Config.config(config), name);
    }

    /**
     * Create a new instance from the configuration located
     * on the provided node.
     *
     * @param config located at {@link #configKey()} node
     * @param name name of the configured implementation
     *
     * @return a new instance created from this config node
     */
    default T create(Config config, String name) {
        // default to preserve backward compatibility
        // require the deprecated variant to be implemented
        DeprecationSupport.requireOverride(this, ConfiguredProvider.class, "create", io.helidon.common.config.Config.class);
        return create((io.helidon.common.config.Config) config, name);
    }
}
