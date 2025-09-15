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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.service.registry.Service;

/**
 * Mutable config override.
 * This class is an internal API used to support the deprecated global config mutability.
 *
 * @deprecated Use {@link io.helidon.service.registry.Services#get(Class)}, or
 *         {@link io.helidon.service.registry.ServiceRegistry#get(Class)}
 */
@Service.Singleton
@Deprecated(forRemoval = true, since = "4.3.0")
@SuppressWarnings({"removal", "DeprecatedIsStillUsed"})
public final class ConfigRef {
    private static final System.Logger LOGGER = System.getLogger(ConfigRef.class.getName());

    private final AtomicReference<Config> ref = new AtomicReference<>();
    private final AtomicBoolean logged = new AtomicBoolean(false);

    /**
     * Test if the config reference is set.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean isSet() {
        return ref.get() != null;
    }

    ConfigRef() {
    }

    /**
     * Get the config reference.
     *
     * @return Config
     * @throws java.util.NoSuchElementException if the reference is not set
     */
    public Config get() {
        Config config = ref.get();
        if (config != null) {
            return config;
        }
        throw new NoSuchElementException("Config reference not set");
    }

    void set(Config config) {
        ref.set(config);
        if (logged.compareAndSet(false, true)) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Attempting to set a config instance when it either was already "
                    + "set once, or it was already used by a component. "
                    + "This will not work in future versions of Helidon");
        }
    }
}
