/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.util.Objects;
import java.util.function.Supplier;

import io.helidon.service.registry.Services;

/**
 * Global configuration facade.
 * <p>
 * The global config instance is maintained in {@link io.helidon.service.registry.GlobalServiceRegistry} and is statically
 * available.
 * </p>
 * <p>
 * NOTE: Setting the global config instance after the first access is a deprecated behavior and will result in WARNING
 * log messages. Use {@link io.helidon.service.registry.Services#set(Class, Object[])} instead.
 * </p>
 *
 * @deprecated The package {@link io.helidon.common.config} will be removed in the next major version.
 */
@Deprecated(forRemoval = true, since = "4.2.0")
@SuppressWarnings({"removal", "DeprecatedIsStillUsed"})
public final class GlobalConfig {

    private GlobalConfig() {
    }

    /**
     * Whether a global configuration has already been configured.
     *
     * @return {@code true} if there is a global configuration set already, {@code false} otherwise
     */
    public static boolean configured() {
        return Services.contains(Config.class);
    }

    /**
     * Get the "global" config instance.
     * <p>
     * The "global" config instance is maintained in {@link io.helidon.service.registry.GlobalServiceRegistry} and is statically
     * available.
     * </p>
     * <p>
     * A custom instance can be registered before the first access using
     * {@link io.helidon.service.registry.Services#set(Class, Object[])},
     * </p>
     *
     * @return "global" config instance
     * @see #config(java.util.function.Supplier)
     * @see #config(java.util.function.Supplier, boolean)
     * @deprecated use {@link io.helidon.service.registry.Services#get(Class)} instead
     */
    @Deprecated(forRemoval = true, since = "4.2.0")
    public static Config config() {
        return Services.get(Config.class);
    }

    /**
     * Set the global config instance.
     * <p>
     * NOTE: Setting the global config instance after the first access is a deprecated behavior and will result in WARNING
     * log messages. Use {@link io.helidon.service.registry.Services#set(Class, Object[])} instead.
     * </p>
     *
     * @param supplier config supplier
     * @return "global" config instance
     * @deprecated use {@link io.helidon.service.registry.Services#set(Class, Object[])} to set the global config instance
     */
    @Deprecated(forRemoval = true, since = "4.2.0")
    public static Config config(Supplier<Config> supplier) {
        return config(supplier, false);
    }

    /**
     * Set the global config instance.
     * <p>
     * NOTE: Setting the global config instance after the first access is a deprecated behavior and will result in WARNING
     * log messages. Use {@link io.helidon.service.registry.Services#set(Class, Object[])} instead.
     * </p>
     *
     * @param supplier  config supplier
     * @param overwrite whether to overwrite an existing configured value
     * @return current global config
     * @deprecated use {@link io.helidon.service.registry.Services#set(Class, Object[])} to set the global config instance
     */
    @Deprecated(forRemoval = true, since = "4.2.0")
    public static Config config(Supplier<Config> supplier, boolean overwrite) {
        Objects.requireNonNull(supplier);
        if (overwrite || !configured()) {
            Config config = supplier.get();
            try {
                Services.set(Config.class, config);
            } catch (Exception e) {
                Services.get(ConfigRef.class).set(config);
            }
            return config;
        }
        return Services.get(Config.class);
    }
}
