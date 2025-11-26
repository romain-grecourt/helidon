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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.GenericType;
import io.helidon.common.context.Context;
import io.helidon.service.registry.Services;
import io.helidon.testing.junit5.TestJunitExtension;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.HelidonJunitExtension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.common.context.Contexts.runInContext;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.requireStatic;

class HelidonJunitExtensionBase<T extends HelidonJunitExtension>
        implements HelidonJunitExtension, ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(HelidonJunitExtensionBase.class);
    private static final GenericType<List<ServerFeature>> FEATURES_TYPE = new GenericType<>() {
    };

    private final List<T> extensions;
    private final Set<Class<?>> paramTypes;

    HelidonJunitExtensionBase(Class<T> extensionType, Set<Class<?>> paramTypes) {
        this.extensions = Services.all(extensionType);
        this.paramTypes = paramTypes;
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
        runInContext(staticContext, () -> extensions.forEach(it -> it.afterAll(ec)));
    }

    @Override
    public void beforeEach(ExtensionContext ec) {
        var staticContext = staticContext(ec);
        runInContext(staticContext, () -> extensions.forEach(it -> it.beforeEach(ec)));
    }

    @Override
    public void afterEach(ExtensionContext ec) {
        var staticContext = staticContext(ec);
        runInContext(staticContext, () -> extensions.forEach(it -> it.afterEach(ec)));
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec)
            throws ParameterResolutionException {

        var staticContext = staticContext(ec);
        return runInContext(staticContext, () -> {
            Class<?> paramType = pc.getParameter().getType();
            if (paramTypes.contains(paramType)) {
                return true;
            }
            for (var ext : extensions()) {
                if (ext.supportsParameter(pc, ec)) {
                    return true;
                }
            }
            var value = staticContext.get(paramType).orElse(null);
            return value != null;
        });
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec)
            throws ParameterResolutionException {

        var staticContext = staticContext(ec);
        return runInContext(staticContext, () -> resolve(pc, ec));
    }

    /**
     * Resolve a parameter.
     *
     * @param pc  parameter context
     * @param ctx extension context
     * @return resolved parameter
     */
    protected Object resolve(ParameterContext pc, ExtensionContext ctx) {
        var paramType = pc.getParameter().getType();
        throw new ParameterResolutionException(
                "Failed to resolve parameter of type " + paramType.getName());
    }

    /**
     * Get the extensions.
     *
     * @return extensions
     */
    protected List<T> extensions() {
        return extensions;
    }

    /**
     * Initialize the test class resources.
     *
     * @param ctx           JUnit extension context
     * @param staticContext Helidon static context
     */
    protected void initClass(ExtensionContext ctx, Context staticContext) {
        // no-op
    }

    /**
     * Hook invoked before the {@link io.helidon.webserver.testing.junit5.AfterStop} annotated methods.
     */
    protected void beforeClose() {
        // no-op
    }

    /**
     * Hook invoked after the {@link io.helidon.webserver.testing.junit5.AfterStop} annotated methods.
     */
    protected void afterClose() {
        // no-op
    }

    /**
     * Get or initialize the static context.
     *
     * @param ec extension context
     * @return static context
     */
    protected Context staticContext(ExtensionContext ec) {
        var store = ec.getStore(NAMESPACE);
        var classContext = store.get(ClassContext.class, ClassContext.class);
        if (classContext == null) {
            throw new IllegalStateException("test context not found");
        }
        return classContext.staticContext();
    }

    /**
     * Set up the {@link WebServerConfig}.
     *
     * @param builder   webserver builder
     * @param testClass test class
     */
    protected static void setupServer(WebServerConfig.Builder builder, Class<?> testClass) {
        var annotated = annotated(methods(testClass));
        var elements = filterAnnotated(annotated, SetUpServer.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length != 1) {
                    throw new IllegalArgumentException(
                            "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                    m, WebServerConfig.Builder.class.getCanonicalName()));
                }
                if (!paramTypes[0].equals(WebServerConfig.Builder.class)) {
                    throw new IllegalArgumentException(
                            "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                    m, WebServerConfig.Builder.class.getCanonicalName()));
                }
                invokeMethod(requireStatic(m), null, builder);
            }
        }
    }

    /**
     * Set up the {@link ServerFeature}.
     *
     * @param builder   webserver builder
     * @param testClass test class
     */
    protected static void setupFeatures(WebServerConfig.Builder builder, Class<?> testClass) {
        var annotated = annotated(methods(testClass));
        var elements = filterAnnotated(annotated, SetUpFeatures.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                for (var a : e.annotations()) {
                    if (!a.value()) {
                        builder.featuresDiscoverServices(false);
                    }
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length > 0) {
                        throw new IllegalArgumentException(
                                "Method %s annotated with @SetUpFeatures has parameters".formatted(
                                        m));
                    }

                    try {
                        var result = invokeMethod(FEATURES_TYPE, requireStatic(m), null);
                        for (ServerFeature feature : result) {
                            builder.addFeature(feature);
                        }
                    } catch (ClassCastException ex) {
                        throw new IllegalArgumentException(
                                "Method %s annotated with @SetUpFeatures must return %s".formatted(
                                        m, FEATURES_TYPE), ex);
                    }
                }
            }
        }
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

        Context staticContext() {
            var store = ec.getStore(SHARED_NAMESPACE);
            var staticCtx = store.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class);
            if (staticCtx == null) {
                throw new IllegalStateException(GLOBAL_CONTEXT_CLASSIFIER + " not found");
            }
            if (!initialized) {
                try {
                    lock.lock();
                    if (!initialized) {
                        initialized = true;
                        runInContext(staticCtx, () -> {
                            extensions.forEach(it -> it.beforeAll(ec));
                            initClass(ec, staticCtx);
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
            beforeClose();
            var testClass = ec.getRequiredTestClass();
            for (Method method : testClass.getMethods()) {
                var annot = method.getAnnotation(AfterStop.class);
                if (annot != null) {
                    if (method.getParameterCount() > 0) {
                        throw new IllegalStateException(
                                "Method annotated with @AfterStop has parameters: " + method);
                    }
                    invokeMethod(requireStatic(method), method);
                }
            }
            afterClose();
        }
    }
}
