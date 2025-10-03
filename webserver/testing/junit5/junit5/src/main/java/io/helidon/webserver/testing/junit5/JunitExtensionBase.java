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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.testing.junit5.TestJunitExtension;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.HelidonJunitExtension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static io.helidon.webserver.testing.junit5.Junit5Util.withStaticMethods;

abstract class JunitExtensionBase<T extends HelidonJunitExtension>
        extends TestJunitExtension
        implements BeforeAllCallback,
                   AfterAllCallback,
                   InvocationInterceptor {

    private final List<T> extensions;
    private final Set<Class<?>> paramTypes;

    JunitExtensionBase(Class<T> extensionType, Set<Class<?>> paramTypes) {
        this.extensions = HelidonServiceLoader.create(ServiceLoader.load(extensionType)).asList();
        this.paramTypes = paramTypes;
    }

    @Override
    public final void beforeAll(ExtensionContext ctx) {
        if (System.getProperty("helidon.config.profile") == null && System.getProperty("config.profile") == null) {
            System.setProperty("helidon.config.profile", "test");
        }
        super.beforeAll(ctx);
        var tc = ctx.getRequiredTestClass();
        var hc = staticContext(ctx).orElseThrow();
        var resource = new State(tc, hc, this::init, this::close);
        store(ctx, resource.testClass).put("state", resource);
        run(ctx, () -> extensions().forEach(it -> it.beforeAll(ctx)));
    }

    @Override
    public final void interceptDynamicTest(Invocation<Void> inv, DynamicTestInvocationContext ic, ExtensionContext ctx)
            throws Throwable {

        state(ctx).init();
        inv.proceed();
    }

    @Override
    public final void interceptTestMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ic, ExtensionContext ctx)
            throws Throwable {

        state(ctx).init();
        inv.proceed();
    }

    @Override
    public final void afterAll(ExtensionContext ctx) {
        runChecked(ctx, () -> extensions.forEach(it -> it.afterAll(ctx)));
        super.afterAll(ctx);
    }

    @Override
    public final void beforeEach(ExtensionContext ctx) throws Exception {
        runChecked(ctx, () -> extensions.forEach(it -> it.beforeEach(ctx)));
        super.beforeEach(ctx);
    }

    @Override
    public final void afterEach(ExtensionContext ctx) throws Exception {
        runChecked(ctx, () -> extensions.forEach(it -> it.afterEach(ctx)));
        super.beforeEach(ctx);
    }

    @Override
    public final boolean supportsParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        var state = state(ctx);
        return supplyChecked(ctx, () -> {
            Class<?> paramType = pc.getParameter().getType();
            if (paramTypes.contains(paramType)) {
                return true;
            }
            for (T extension : extensions()) {
                if (extension.supportsParameter(pc, ctx)) {
                    return true;
                }
            }
            var value = state.ctx.get(paramType).orElse(null);
            if (value != null) {
                return true;
            }
            return super.supportsParameter(pc, ctx);
        });
    }

    @Override
    public final Object resolveParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        var state = state(ctx);
        return supplyChecked(ctx, () -> {
            Object resolved = resolve(pc, ctx);
            if (resolved == null) {
                var paramType = pc.getParameter().getType();
                resolved = state.ctx.get(paramType).orElse(null);
            }
            return resolved != null ? resolved : super.resolveParameter(pc, ctx);
        });
    }

    abstract void init(Class<?> testClass, Context ctx);

    abstract Object resolve(ParameterContext pc, ExtensionContext ctx);

    void close(Class<?> testClass) {
        for (Method method : testClass.getMethods()) {
            var annot = method.getAnnotation(AfterStop.class);
            if (annot != null) {
                if (method.getParameterCount() != 0) {
                    throw new IllegalStateException(
                            "Method annotated @AfterStop has parameters: " + method);
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    try {
                        method.invoke(testClass);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to invoke method: " + method, e);
                    }
                } else {
                    throw new IllegalStateException(
                            "Method annotated with @AfterStop is not static: " + method);
                }
            }
        }
    }

    final List<T> extensions() {
        return extensions;
    }

    final void init(ExtensionContext ctx) {
        state(ctx).init();
    }

    protected static void setupServer(WebServerConfig.Builder builder, Class<?> testClass) {
        withStaticMethods(testClass, SetUpServer.class, (setUpServer, method) -> {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                method, WebServerConfig.Builder.class.getCanonicalName()));
            }
            if (!paramTypes[0].equals(WebServerConfig.Builder.class)) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                method, WebServerConfig.Builder.class.getCanonicalName()));
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpServer is not static".formatted(
                                method));
            }
            try {
                method.setAccessible(true);
                method.invoke(null, builder);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Could not invoke method " + method, e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected static void setupFeatures(WebServerConfig.Builder builder, Class<?> testClass) {
        withStaticMethods(testClass, SetUpFeatures.class, (annot, method) -> {
            if (!annot.value()) {
                builder.featuresDiscoverServices(false);
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 0) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpFeatures must only return List<ServerFeature>".formatted(
                                method));
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpFeatures is not static".formatted(
                                method));
            }
            Object result;
            try {
                method.setAccessible(true);
                result = method.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Could not invoke method " + method, e);
            }

            try {
                for (ServerFeature feature : (List<ServerFeature>) result) {
                    builder.addFeature(feature);
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(
                        "Method %s annotated with @SetUpFeatures must return List<ServerFeature>".formatted(
                                method), e);
            }
        });
    }

    private static State state(ExtensionContext ctx) {
        ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
        return storeLookup(store, "state", State.class).orElseThrow();
    }

    private static final class State implements ExtensionContext.Store.CloseableResource {
        private final BiConsumer<Class<?>, Context> initFunc;
        private final Consumer<Class<?>> closeFunc;
        private final Lock lock = new ReentrantLock();
        private final Class<?> testClass;
        private final Context ctx;
        private boolean initialized;
        private RuntimeException error;

        State(Class<?> testClass, Context ctx, BiConsumer<Class<?>, Context> initFunc, Consumer<Class<?>> closeFunc) {
            this.testClass = testClass;
            this.ctx = ctx;
            this.initFunc = initFunc;
            this.closeFunc = closeFunc;
        }

        void init() {
            if (error == null && !initialized) {
                try {
                    lock.lock();
                    if (error == null && !initialized) {
                        Contexts.runInContext(ctx, () -> {
                            initFunc.accept(testClass, ctx);
                            initialized = true;
                        });
                    }
                } catch (RuntimeException ex) {
                    error = ex;
                    throw ex;
                } finally {
                    lock.unlock();
                }
            }
            if (error != null) {
                throw error;
            }
        }

        @Override
        public void close() {
            closeFunc.accept(testClass);
        }
    }
}
