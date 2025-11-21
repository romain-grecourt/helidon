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

import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static io.helidon.common.context.Contexts.runInContext;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.requireStatic;

class JunitExtensionBase<T extends HelidonJunitExtension>
        implements HelidonJunitExtension,
                   InvocationInterceptor,
                   ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(TestJunitExtension.class);
    private static final String GLOBAL_CONTEXT_CLASSIFIER = "helidon-registry-static-context";
    private static final GenericType<List<ServerFeature>> FEATURES_TYPE = new GenericType<>() {
    };

    private final List<T> extensions;
    private final Set<Class<?>> paramTypes;
    private final Lock lock = new ReentrantLock();
    private boolean initialized;

    JunitExtensionBase(Class<T> extensionType, Set<Class<?>> paramTypes) {
        this.extensions = Services.all(extensionType);
        this.paramTypes = paramTypes;
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (System.getProperty("helidon.config.profile") == null && System.getProperty("config.profile") == null) {
            System.setProperty("helidon.config.profile", "test");
        }
    }

    @Override
    public void interceptDynamicTest(Invocation<Void> inv, DynamicTestInvocationContext ic, ExtensionContext ctx)
            throws Throwable {

        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        inv.proceed();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ic, ExtensionContext ctx)
            throws Throwable {

        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        inv.proceed();
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        runInContext(staticContext, () -> extensions.forEach(it -> it.afterAll(ctx)));
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        runInContext(staticContext, () -> extensions.forEach(it -> it.beforeEach(ctx)));
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        runInContext(staticContext, () -> extensions.forEach(it -> it.afterEach(ctx)));
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        return runInContext(staticContext, () -> {
            Class<?> paramType = pc.getParameter().getType();
            if (paramTypes.contains(paramType)) {
                return true;
            }
            for (var ext : extensions()) {
                if (ext.supportsParameter(pc, ctx)) {
                    return true;
                }
            }
            var value = staticContext.get(paramType).orElse(null);
            return value != null;
        });
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        var staticContext = staticContext(ctx);
        init(ctx, staticContext);
        return runInContext(staticContext, () -> resolve(pc, ctx));
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
     * Close a test.
     *
     * @param ctx extension context
     */
    protected void close(ExtensionContext ctx) {
        var testClass = ctx.getRequiredTestClass();
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

    private void init(ExtensionContext ctx, Context staticContext) {
        if (!initialized) {
            try {
                lock.lock();
                if (!initialized) {
                    initialized = true;
                    runInContext(staticContext, () -> {
                        extensions.forEach(it -> it.beforeAll(ctx));
                        initClass(ctx, staticContext);
                    });
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private Context staticContext(ExtensionContext ctx) {
        var store = ctx.getStore(NAMESPACE);
        var staticCtx = store.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class);
        if (staticCtx != null) {
            return staticCtx;
        } else {
            throw new IllegalStateException(GLOBAL_CONTEXT_CLASSIFIER + " not found");
        }
    }

    /**
     * Set up the {@link WebServerConfig}.
     *
     * @param builder   webserver builder
     * @param testClass test class
     */
    protected static void setupServer(WebServerConfig.Builder builder, Class<?> testClass) {
        var annotated = annotated(testClass);
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
        var annotated = annotated(testClass);
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
}
