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

package io.helidon.testing.junit5;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.helidon.common.Functions;
import io.helidon.common.Functions.CheckedSupplier;
import io.helidon.common.GenericType;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;
import io.helidon.service.registry.ServiceRegistryManager;
import io.helidon.testing.TestException;
import io.helidon.testing.TestRegistry;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * A JUnit extension that supports {@link io.helidon.service.registry.ServiceRegistry}.
 * <p>
 * A registry is set up for each test and is usable statically via {@link io.helidon.service.registry.Services}
 * or {@link io.helidon.service.registry.GlobalServiceRegistry#registry()}.
 * <p>
 * The registry can be injected with JUnit as a constructor or method parameter.
 * <p>
 * The {@link io.helidon.testing.junit5.Testing.Test} annotation can be used on test classes to customize the behavior.
 */
public class TestJunitExtension implements Extension,
                                           InvocationInterceptor,
                                           BeforeEachCallback,
                                           AfterEachCallback,
                                           BeforeAllCallback,
                                           AfterAllCallback,
                                           ParameterResolver {

    private static final String GLOBAL_CONTEXT_CLASSIFIER = "helidon-registry-static-context";
    private static final String GLOBAL_REGISTRY_CLASSIFIER = "helidon-registry";
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TestJunitExtension.class);

    static {
        LogConfig.initClass();
    }

    /**
     * Default constructor with no side effects.
     */
    protected TestJunitExtension() {
    }

    @Override
    public void beforeAll(ExtensionContext ec) {
        var store = ec.getStore(NAMESPACE);
        store.put(ClassContext.class, new ClassContext(ec));
    }

    @Override
    public void afterAll(ExtensionContext ec) {
        run(ec, () -> afterShutdownMethods(ec.getRequiredTestClass()));
    }

    @Override
    public void beforeEach(ExtensionContext ec) {
        var testClass = ec.getRequiredTestClass();
        var methodName = ec.getRequiredTestMethod().getName();
        var annot = testClass.getAnnotation(Testing.Test.class);
        if (annot != null && annot.perMethod()) {
            var store = ec.getStore(NAMESPACE);
            var staticContext = staticContext(ec);
            var methodStaticContext = newStaticContext(testClass, methodName);
            staticContext.register(GLOBAL_CONTEXT_CLASSIFIER, methodStaticContext);
            store.put(GlobalRegistry.class, GlobalRegistry.create(methodStaticContext));
        }
    }

    @Override
    public void afterEach(ExtensionContext ec) throws Exception {
        var testClass = ec.getRequiredTestClass();
        var annot = testClass.getAnnotation(Testing.Test.class);
        if (annot != null && annot.perMethod()) {
            var staticContext = staticContext(ec);
            staticContext.register(GLOBAL_CONTEXT_CLASSIFIER, staticContext);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec)
            throws ParameterResolutionException {

        return supplyChecked(ec, () -> {
            var paramType = pc.getParameter().getType();
            var genericParamType = GenericType.create(pc.getParameter().getParameterizedType());
            if (!genericParamType.isClass()) {
                return false;
            }
            return supportedType(GlobalServiceRegistry.registry(), paramType);
        });
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec)
            throws ParameterResolutionException {

        return supplyChecked(ec, () -> {
            var paramType = pc.getParameter().getType();
            var registry = GlobalServiceRegistry.registry();
            if (supportedType(registry, paramType)) {
                return registry.get(paramType);
            }
            throw new ParameterResolutionException("Failed to resolve parameter of type " + paramType.getName());
        });
    }

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
                                               ReflectiveInvocationContext<Constructor<T>> ic,
                                               ExtensionContext ec) throws Throwable {
        return invoke(ec, invocation);
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> ic,
                                         ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> ic,
                                          ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> ic,
                                    ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
                                            ReflectiveInvocationContext<Method> ic,
                                            ExtensionContext ec) throws Throwable {
        return invoke(ec, invocation);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> ic,
                                            ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public void interceptDynamicTest(Invocation<Void> invocation,
                                     DynamicTestInvocationContext ic,
                                     ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> ic,
                                         ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation,
                                        ReflectiveInvocationContext<Method> ic,
                                        ExtensionContext ec) throws Throwable {
        invoke(ec, invocation);
    }

    /**
     * Initialize the "static" context.
     *
     * @param ec extension context
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("unused")
    protected void initStaticContext(ExtensionContext ec) {
    }

    /**
     * Initialize the "static" context.
     *
     * @param store extension store
     * @param ec    extension context
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("unused")
    protected void initStaticContext(Store store, ExtensionContext ec) {
    }

    /**
     * Get the "static" context.
     *
     * @param ec extension context
     * @return static context
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected Context staticContext(ExtensionContext ec) {
        var store = ec.getStore(NAMESPACE);
        ClassContext classContext = store.get(ClassContext.class, ClassContext.class);
        if (classContext == null) {
            throw new IllegalStateException("test context not found");
        }
        return classContext.staticContext();
    }

    /**
     * Call a supplier within context.
     *
     * @param ec       extension context
     * @param supplier callable to invoke
     * @param <T>      type of the result
     * @return result of the callable
     * @throws Throwable in case the call to callable threw an exception
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    protected <T> T supply(ExtensionContext ec, Supplier<T> supplier) throws Throwable {
        return Contexts.runInContext(staticContext(ec), supplier::get);
    }

    /**
     * Call a supplier that can throw {@link Throwable} within context.
     *
     * @param ec       extension context
     * @param supplier supplier to invoke
     * @param <T>      type of the result
     * @param <E>      type of checked exception that is expected
     * @return result of the callable
     * @throws E in case the call to callable threw an exception
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("unchecked")
    protected <T, E extends Throwable> T supplyChecked(ExtensionContext ec, CheckedSupplier<T, E> supplier) throws E {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        T response = Contexts.runInContext(staticContext(ec), () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                thrown.set(e);
                return null;
            }
        });
        if (thrown.get() == null) {
            return response;
        }
        var throwable = thrown.get();
        if (throwable instanceof RuntimeException rte) {
            throw rte;
        }
        if (throwable instanceof Error err) {
            throw err;
        }
        throw (E) throwable;
    }

    /**
     * Run a runnable within context.
     *
     * @param ctx      extension context
     * @param runnable runnable to run
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    protected void run(ExtensionContext ctx, Runnable runnable) {
        Contexts.runInContext(staticContext(ctx), runnable);
    }

    /**
     * Invoke a runnable that may throw a checked exception.
     *
     * @param ctx      extension context
     * @param runnable runnable to run
     * @param <E>      type of the exception that can be thrown
     * @throws E in case the runnable threw an exception
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("unchecked")
    protected <E extends Throwable> void runChecked(ExtensionContext ctx, Functions.CheckedRunnable<E> runnable) throws E {
        AtomicReference<Throwable> thrown = new AtomicReference<>();

        Contexts.runInContext(staticContext(ctx), () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                thrown.set(e);
            }
        });

        if (thrown.get() == null) {
            return;
        }
        var throwable = thrown.get();
        if (throwable instanceof RuntimeException rte) {
            throw rte;
        }
        if (throwable instanceof Error err) {
            throw err;
        }
        throw (E) throwable;
    }

    /**
     * Invoke a JUnit invocation within context.
     *
     * @param ctx        extension context
     * @param invocation invocation to invoke
     * @param <T>        type of the returned value
     * @return result of the invocation
     * @throws Throwable in case the invocation threw an exception
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    protected <T> T invoke(ExtensionContext ctx, Invocation<T> invocation) throws Throwable {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        T response = Contexts.runInContext(staticContext(ctx), () -> {
            try {
                return invocation.proceed();
            } catch (Throwable e) {
                thrown.set(e);
                return null;
            }
        });
        if (thrown.get() != null) {
            throw thrown.get();
        }
        return response;
    }

    /**
     * Get an object from the given store.
     *
     * @param store store
     * @param key   key
     * @param type  type
     * @param <T>   object type
     * @return optional
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("SameParameterValue")
    protected static <T> Optional<T> storeLookup(Store store, Object key, Class<T> type) {
        return Optional.ofNullable(store.get(key, type));
    }

    /**
     * Get an extension store.
     *
     * @param ec         extension context
     * @param qualifiers qualifiers
     * @return extension store
     * @deprecated this extension is now standalone and should not be extended. All protected methods will be removed in the
     *         next major release.
     */
    @Deprecated(forRemoval = true, since = "4.4.0")
    @SuppressWarnings("DuplicatedCode")
    protected static Store store(ExtensionContext ec, AnnotatedElement... qualifiers) {
        Namespace ns;
        if (qualifiers.length > 0) {
            ns = NAMESPACE.append(Arrays.stream(qualifiers)
                    .map(e -> switch (e) {
                        case Class<?> c -> c.getName();
                        case Method m -> m.getName();
                        default -> throw new IllegalArgumentException("Unsupported element: " + e);
                    })
                    .toArray());
        } else {
            ns = NAMESPACE;
        }
        return ec.getStore(ns);
    }

    private static Context newStaticContext(Class<?> testClass, String methodName) {
        var suffix = methodName != null ? "." + methodName : "";
        var staticCtx = Context.builder()
                .id("test-" + testClass.getName() + "-" + System.identityHashCode(testClass) + suffix)
                .build();
        staticCtx.register(GLOBAL_CONTEXT_CLASSIFIER, staticCtx);
        return staticCtx;
    }

    private static boolean isRegistrySet(Context context) {
        return context.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class)
                .flatMap(ctx -> ctx.get(GLOBAL_REGISTRY_CLASSIFIER, ServiceRegistry.class))
                .isPresent();
    }

    private static void afterShutdownMethods(Class<?> requiredTestClass) {
        for (Method declaredMethod : requiredTestClass.getDeclaredMethods()) {
            var annotation = declaredMethod.getAnnotation(TestRegistry.AfterShutdown.class);
            if (annotation != null) {
                if (!Modifier.isStatic(declaredMethod.getModifiers())) {
                    throw new TestException("Cannot invoke @TestRegistry.AfterShutdown annotated method "
                                            + declaredMethod.getName() + ", as it is not static");
                }

                try {
                    declaredMethod.setAccessible(true);
                    declaredMethod.invoke(null);
                } catch (Exception e) {
                    throw new TestException("Failed to invoke @TestRegistry.AfterShutdown annotated method "
                                            + declaredMethod.getName(), e);

                }
            }
        }
    }

    private static boolean supportedType(ServiceRegistry registry, Class<?> paramType) {
        if (ServiceRegistry.class.isAssignableFrom(paramType)) {
            return true;
        }
        return !registry.allServices(paramType).isEmpty();
    }

    private record GlobalRegistry(Context context, ServiceRegistryManager manager) implements CloseableResource {

        static GlobalRegistry create(Context context) {
            var manager = ServiceRegistryManager.create();
            context.register(GLOBAL_REGISTRY_CLASSIFIER, manager.registry());
            return new GlobalRegistry(context, manager);
        }

        @Override
        public void close() {
            if (manager != null) {
                manager.shutdown();
            }
        }
    }

    private static class ClassContext implements CloseableResource {

        private final ExtensionContext ec;
        private final Lock lock = new ReentrantLock();
        private GlobalRegistry registry;

        private ClassContext(ExtensionContext ec) {
            this.ec = ec;
        }

        @Override
        public void close() {
            if (registry != null) {
                registry.close();
            }
        }

        Context staticContext() {
            if (registry == null) {
                try {
                    lock.lock();
                    if (registry == null) {
                        var globalStore = ec.getStore(GLOBAL);
                        var staticCtx = globalStore.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class);
                        if (staticCtx == null) {
                            var testClass = ec.getRequiredTestClass();
                            var store = ec.getStore(NAMESPACE);
                            staticCtx = newStaticContext(testClass, null);
                            store.put(GLOBAL_CONTEXT_CLASSIFIER, staticCtx);
                            if (isRegistrySet(staticCtx)) {
                                registry = new GlobalRegistry(staticCtx, null);
                            } else {
                                registry = GlobalRegistry.create(staticCtx);
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
            return registry.context;
        }
    }
}
