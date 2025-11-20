/*
 * Copyright (c) 2019, 2025 Oracle and/or its affiliates.
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

import io.helidon.service.registry.Services;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * Integration with microprofile config.
 * This class is an implementation of a java service obtained through ServiceLoader.
 */
public class MpConfigProviderResolver extends ConfigProviderResolver {

    /**
     * This method should only be called when running within native image, as soon as runtime configuration
     * is available.
     *
     * @param config configuration to use
     */
    public static void runtimeStart(Config config) {
        Services.get(MpConfigFactory.class).runtimeStart(config);
    }

    /**
     * This method should only be called when generating native image, as late in the process as possible.
     */
    public static void buildTimeEnd() {
        Services.get(MpConfigFactory.class).buildTimeEnd();
    }

    @Override
    public ConfigBuilder getBuilder() {
        return new MpConfigBuilder();
    }

    @Override
    public Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader cl) {
        ClassLoader loader = cl == null ? Thread.currentThread().getContextClassLoader() : cl;
        return Services.get(MpConfigFactory.class).getConfig(loader);
    }

    @Override
    public void registerConfig(Config config, ClassLoader cl) {
        ClassLoader loader = cl == null ? Thread.currentThread().getContextClassLoader() : cl;
        Services.get(MpConfigFactory.class).registerConfig(l -> config, loader);
    }

    @Override
    public void releaseConfig(Config config) {
        Services.get(MpConfigFactory.class).releaseConfig(config);
    }
}
