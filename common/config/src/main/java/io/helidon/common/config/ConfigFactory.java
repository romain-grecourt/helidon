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

import java.util.function.Supplier;

import io.helidon.common.LazyValue;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;

/**
 * Default {@link Config} factory.
 * This factory exists as a fallback if there is no config implementation on classpath.
 * <br>
 * The created config instance is a delegate that honors the current "global" config.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT - 20)
@SuppressWarnings({"removal", "ClassCanBeRecord"})
class ConfigFactory implements Supplier<Config> {

    private final LazyValue<Config> defaultConfig;
    private final ConfigRef ref;

    @Service.Inject
    ConfigFactory(io.helidon.common.config.spi.ConfigProvider provider, ConfigRef ref) {
        this.defaultConfig = LazyValue.create(provider::create);
        this.ref = ref;
    }

    @Override
    public Config get() {
        return new ConfigDelegate(this::delegate);
    }

    private Config delegate() {
        return ref.isSet() ? ref.get() : defaultConfig.get();
    }

    static Config createDefault() {
        return Services.first(io.helidon.common.config.spi.ConfigProvider.class)
                .map(io.helidon.common.config.spi.ConfigProvider::create)
                .orElse(Config.empty());
    }
}
