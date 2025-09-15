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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.config.Config;
import io.helidon.config.MetaConfig;
import io.helidon.service.registry.Service;

/**
 * {@link io.helidon.config.Config} factory backed by MicroProfile Config.
 * <br>
 * Uses a weight higher than default to force the use of MicroProfile config.
 * <br>
 * The created config instance is a delegate that honors the current "global" config.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 1)
@SuppressWarnings("removal")
class MpConfigProvider implements Supplier<Config> {
    private static final System.Logger LOGGER = System.getLogger(MpConfigProviderResolver.class.getName());

    private final Map<ClassLoader, CompositeConfig.MpDelegate> configs = new IdentityHashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final List<CompositeConfig.MpDelegate> stash = new ArrayList<>();

    private final ConfigRef ref;

    @Service.Inject
    MpConfigProvider(ConfigRef ref) {
        this.ref = ref;
    }

    @Override
    public Config get() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    CompositeConfig getConfig(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader is null");
        if (ref.isSet()) {
            return ref.get();
        }
        Lock lock = rwLock.readLock();
        CompositeConfig config;
        try {
            lock.lock();
            config = configs.get(loader);
        } finally {
            lock.unlock();
        }
        if (config != null) {
            return config;
        } else {
            return registerConfig(this::buildConfig, loader);
        }
    }

    CompositeConfig registerConfig(Function<ClassLoader, org.eclipse.microprofile.config.Config> function, ClassLoader loader) {
        Objects.requireNonNull(function, "function is null");
        Objects.requireNonNull(loader, "loader is null");
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            var config = function.apply(loader);
            if (config instanceof CompositeConfig composite) {
                config = composite.mpConfig();
            }
            CompositeConfig.MpDelegate newConfig = new CompositeConfig.MpDelegate(config);
            CompositeConfig.MpDelegate oldConfig = configs.put(loader, newConfig);
            if (oldConfig != null) {
                oldConfig.set(config);
            }
            return newConfig;
        } finally {
            lock.unlock();
        }
    }

    org.eclipse.microprofile.config.Config buildConfig(ClassLoader loader) {
        MpConfigBuilder builder = new MpConfigBuilder();
        builder.forClassLoader(loader);

        Optional<Config> meta = MpMetaConfig.metaConfig();
        if (meta.isEmpty()) {
            meta = MetaConfig.metaConfig();
            if (meta.isPresent()) {
                builder.metaConfig(meta.get());
                LOGGER.log(System.Logger.Level.WARNING,
                        "You are using Helidon SE meta configuration in a Helidon MP application. Some "
                        + "features work differently, such as environment variable resolving, and mutability");
            }
        } else {
            builder.mpMetaConfig(meta.get());
        }

        // use defaults if no meta config
        if (meta.isEmpty()) {
            builder.addDefaultSources();
            builder.addDiscoveredSources();
            builder.addDiscoveredConverters();
        }
        return builder.build();
    }

    void releaseConfig(org.eclipse.microprofile.config.Config config) {
        // first attempt to find it
        Lock lock = rwLock.readLock();
        AtomicReference<ClassLoader> cl = new AtomicReference<>();

        // in case we get our own delegate
        // we want to remove the exact same instance of delegate
        try {
            lock.lock();
            if (config instanceof CompositeConfig) {
                for (Map.Entry<ClassLoader, CompositeConfig.MpDelegate> entry : configs.entrySet()) {
                    if (config == entry.getValue()) {
                        cl.set(entry.getKey());
                        break;
                    }
                }
            } else {
                for (Map.Entry<ClassLoader, CompositeConfig.MpDelegate> entry : configs.entrySet()) {
                    org.eclipse.microprofile.config.Config fromRef = entry.getValue().mpConfig();
                    if (config == fromRef) {
                        cl.set(entry.getKey());
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        // if found, remove it
        if (cl.get() != null) {
            lock = rwLock.writeLock();
            try {
                lock.lock();
                configs.remove(cl.get());
            } finally {
                lock.unlock();
            }
        }
    }

    void runtimeStart(org.eclipse.microprofile.config.Config config) {
        if (!stash.isEmpty()) {
            Iterator<CompositeConfig.MpDelegate> it = stash.iterator();
            while (it.hasNext()) {
                it.next().set(config);
                it.remove();
            }
        }
    }

    void buildTimeEnd() {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            stash.addAll(configs.values());
            configs.clear();
        } finally {
            lock.unlock();
        }
    }
}
