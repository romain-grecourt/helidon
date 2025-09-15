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

import java.util.concurrent.locks.ReentrantLock;

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
public class ConfigRef {

    private final io.helidon.common.config.ConfigRef ref;
    private final ReentrantLock lock = new ReentrantLock();
    private CommonConfig commonConfig;

    @Service.Inject
    ConfigRef(io.helidon.common.config.ConfigRef ref) {
        this.ref = ref;
    }

    /**
     * Test if the config reference is set.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean isSet() {
        return ref.isSet();
    }

    /**
     * Get the config reference.
     *
     * @return Config
     * @throws java.util.NoSuchElementException if the reference is not set
     */
    public Config get() {
        io.helidon.common.config.Config config = ref.get();
        if (config instanceof Config hc) {
            return hc;
        }
        if (commonConfig == null || commonConfig.delegate() != config) {
            try {
                lock.lock();
                if (commonConfig == null || commonConfig.delegate() != config) {
                    commonConfig = new CommonConfig(config);
                }
            } finally {
                lock.unlock();
            }
        }
        return commonConfig;
    }
}
