/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

package io.helidon.webserver.testing.junit5;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.context.Context;
import io.helidon.service.registry.Services;
import io.helidon.testing.junit5.TestJunitExtension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.common.context.Contexts.runInContext;

/**
 * JUnit5 extension base class to support Helidon WebServer unit tests.
 * <p>
 * All methods are delegated to one implementation of {@link WebServerTestService} and wrapped in the "static" context
 * managed by {@link TestJunitExtension}.
 */
class WebServerTestExtension implements BeforeAllCallback,
                                        AfterAllCallback,
                                        BeforeEachCallback,
                                        AfterEachCallback,
                                        ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(WebServerTestExtension.class);
    private final Class<? extends WebServerTestService<?>> serviceClass;

    WebServerTestExtension(Class<? extends WebServerTestService<?>> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public void beforeAll(ExtensionContext ec) {
        if (System.getProperty("helidon.config.profile") == null && System.getProperty("config.profile") == null) {
            System.setProperty("helidon.config.profile", "test");
        }
        var store = ec.getStore(NAMESPACE);
        store.put(ClassContext.class, new ClassContext(ec));
    }

    @Override
    public void afterAll(ExtensionContext ec) {
        var staticContext = staticContext(ec);
        runInContext(staticContext, () -> {
            var svc = Services.get(serviceClass);
            for (var e : svc.extensions()) {
                e.afterAll(ec);
            }
        });
    }

    @Override
    public void beforeEach(ExtensionContext ec) {
        var staticContext = staticContext(ec);
        runInContext(staticContext, () -> {
            var svc = Services.get(serviceClass);
            for (var e : svc.extensions()) {
                e.beforeEach(ec);
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext ec) {
        var staticContext = staticContext(ec);
        runInContext(staticContext, () -> {
            var svc = Services.get(serviceClass);
            for (var e : svc.extensions()) {
                e.afterEach(ec);
            }
        });
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        var staticContext = staticContext(ec);
        return runInContext(staticContext, () -> {
            var svc = Services.get(serviceClass);
            if (svc.supportsParameter(pc, ec)) {
                return true;
            }
            for (var e : svc.extensions()) {
                if (e.supportsParameter(pc, ec)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        var staticContext = staticContext(ec);
        return runInContext(staticContext, () -> Services.get(serviceClass).resolveParameter(pc, ec));
    }

    private Context staticContext(ExtensionContext ec) {
        var store = ec.getStore(NAMESPACE);
        var cc = store.get(ClassContext.class, ClassContext.class);
        if (cc == null) {
            throw new IllegalStateException("test context not found");
        }
        return cc.initStaticContext();
    }

    private final class ClassContext implements CloseableResource {

        private static final Namespace SHARED_NAMESPACE = Namespace.create(TestJunitExtension.class);
        private static final String GLOBAL_CONTEXT_CLASSIFIER = "helidon-registry-static-context";

        private final ExtensionContext ec;
        private final Lock lock = new ReentrantLock();
        private boolean initialized;

        ClassContext(ExtensionContext ec) {
            this.ec = ec;
        }

        Context staticContext(ExtensionContext ec) {
            var store = ec.getStore(SHARED_NAMESPACE);
            return store.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class);
        }

        Context initStaticContext() {
            var staticCtx = staticContext(ec);
            if (staticCtx == null) {
                throw new IllegalStateException(GLOBAL_CONTEXT_CLASSIFIER + " not found");
            }
            if (!initialized) {
                try {
                    lock.lock();
                    if (!initialized) {
                        initialized = true;
                        runInContext(staticCtx, () -> {
                            Services.set(WebServerTestClass.class, new WebServerTestClass(ec.getRequiredTestClass()));
                            var svc = Services.get(serviceClass);
                            for (var e : svc.extensions()) {
                                e.beforeAll(ec);
                            }
                        });
                    }
                } finally {
                    lock.unlock();
                }
            }
            return staticCtx;
        }

        @Override
        public void close() {
            var staticCtx = staticContext(ec);
            if (staticCtx != null) {
                runInContext(staticCtx, () -> Services.get(serviceClass).stop());
            }
        }
    }
}
