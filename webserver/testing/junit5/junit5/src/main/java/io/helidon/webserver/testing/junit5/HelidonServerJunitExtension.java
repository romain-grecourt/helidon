/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
import java.lang.reflect.Parameter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.testing.virtualthreads.PinningRecorder;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Services;
import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerService__ServiceDescriptor;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension.ParamHandler;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static io.helidon.webserver.WebServer.DEFAULT_SOCKET_NAME;
import static io.helidon.webserver.testing.junit5.Junit5Util.withStaticMethods;

/**
 * JUnit5 extension to support Helidon WebServer in tests.
 */
@SuppressWarnings("removal")
class HelidonServerJunitExtension extends JunitExtensionBase
        implements BeforeAllCallback,
                   AfterAllCallback,
                   AfterEachCallback,
                   InvocationInterceptor,
                   ParameterResolver {

    private final List<ServerJunitExtension> extensions;

    HelidonServerJunitExtension() {
        this.extensions = HelidonServiceLoader.create(ServiceLoader.load(ServerJunitExtension.class)).asList();
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        super.beforeAll(ctx);
        Class<?> testClass = ctx.getRequiredTestClass();
        Context hc = staticContext(ctx).orElseThrow();
        ExtensionContext.Store store = store(ctx, testClass);
        store.put("test-server", new TestServer(testClass, hc));
        run(ctx, () -> extensions.forEach(it -> it.beforeAll(ctx)));
    }

    @Override
    public void afterAll(ExtensionContext ctx) {
        run(ctx, () -> {
            extensions.forEach(it -> it.afterAll(ctx));
            ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
            TestServer ts = storeLookup(store, "test-server", TestServer.class).orElseThrow();
            if (ts.server != null) {
                ts.server.stop();
            }
            super.afterAll(ctx);
            if (ts.pinningRecorder != null) {
                ts.pinningRecorder.close();
            }
        });
    }

    @Override
    public void interceptDynamicTest(Invocation<Void> inv, DynamicTestInvocationContext ic, ExtensionContext ctx)
            throws Throwable {

        ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
        TestServer ts = storeLookup(store, "test-server", TestServer.class).orElseThrow();
        ts.server();
        inv.proceed();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> inv, ReflectiveInvocationContext<Method> ic, ExtensionContext ctx)
            throws Throwable {

        ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
        TestServer ts = storeLookup(store, "test-server", TestServer.class).orElseThrow();
        ts.server();
        inv.proceed();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        runChecked(extensionContext, () -> extensions.forEach(it -> it.afterEach(extensionContext)));
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
        TestServer ts = storeLookup(store, "test-server", TestServer.class).orElseThrow();
        return supplyChecked(ctx, () -> {
            Class<?> paramType = pc.getParameter().getType();
            if (paramType.equals(WebServer.class)) {
                return true;
            }
            if (paramType.equals(URI.class)) {
                return true;
            }

            for (ServerJunitExtension extension : extensions) {
                if (extension.supportsParameter(pc, ctx)) {
                    return true;
                }
            }

            var value = ts.ctx.get(paramType).orElse(null);
            if (value != null) {
                return true;
            }
            return super.supportsParameter(pc, ctx);
        });
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        ExtensionContext.Store store = store(ctx, ctx.getRequiredTestClass());
        TestServer ts = storeLookup(store, "test-server", TestServer.class).orElseThrow();
        return supplyChecked(ctx, () -> {
            Class<?> paramType = pc.getParameter().getType();
            if (paramType.equals(WebServer.class)) {
                return ts.server();
            }
            if (paramType.equals(URI.class)) {
                String socketName = Junit5Util.socketName(pc.getParameter());
                URI uri = ts.uri(socketName);
                if (uri == null) {
                    throw new IllegalStateException("Socket not found: " + socketName);
                }
                return uri;
            }

            for (ServerJunitExtension extension : extensions) {
                if (extension.supportsParameter(pc, ctx)) {
                    return extension.resolveParameter(pc, ctx, paramType, ts.server());
                }
            }

            var value = ts.ctx.get(paramType).orElse(null);
            if (value != null) {
                return value;
            }
            return super.resolveParameter(pc, ctx);
        });
    }

    private void handleParams(Method method,
                              String socket,
                              WebServerConfig.Builder server,
                              ListenerConfig.Builder listener,
                              Router.RouterBuilder<?> router) {

        List<ParamHandler<?>> handlers = paramHandlers(method);
        Object[] values = new Object[handlers.size()];

        for (int i = 0; i < handlers.size(); i++) {
            var handler = handlers.get(i);
            values[i] = handler.get(socket, server, listener, router);
        }

        try {
            method.setAccessible(true);
            method.invoke(null, values);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot invoke @SetUpServer method", e);
        }

        for (int i = 0; i < values.length; i++) {
            handleParam(handlers.get(i), socket, server, listener, router, values[i]);
        }
    }

    private List<ParamHandler<?>> paramHandlers(Method method) {
        List<ParamHandler<?>> handlers = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            Class<?> paramType = parameter.getType();
            boolean found = false;
            for (ServerJunitExtension e : extensions) {
                var handler = e.setUpRouteParamHandler(paramType).orElse(null);
                if (handler != null) {
                    handlers.add(handler);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "Method %s has a parameter %s that is not supported"
                                .formatted(method, paramType));
            }
        }
        return handlers;
    }

    @SuppressWarnings("unchecked")
    private static <T> void handleParam(ParamHandler<T> handler,
                                        String socket,
                                        WebServerConfig.Builder server,
                                        ListenerConfig.Builder listener,
                                        Router.RouterBuilder<?> router,
                                        Object value) {

        handler.handle(socket, server, listener, router, (T) value);
    }

    @SuppressWarnings("deprecation")
    private final class TestServer {
        private final Map<String, URI> uris = new ConcurrentHashMap<>();
        private final Lock lock = new ReentrantLock();
        private final Class<?> testClass;
        private final Context ctx;
        private WebServer server;
        private RuntimeException error;
        private PinningRecorder pinningRecorder;

        TestServer(Class<?> testClass, Context ctx) {
            this.testClass = testClass;
            this.ctx = ctx;
        }

        URI uri(String socket) {
            return uris.computeIfAbsent(socket, it -> {
                int port = server.port(it);
                if (port == -1) {
                    return null;
                }
                if (server.hasTls(it)) {
                    return URI.create("https://localhost:" + port + "/");
                }
                return URI.create("http://localhost:" + port + "/");
            });
        }

        WebServer server() {
            if (error == null && server == null) {
                try {
                    lock.lock();
                    if (error == null && server == null) {
                        Contexts.runInContext(ctx, this::init);
                    }
                } catch (RuntimeException ex) {
                    error = ex;
                    throw ex;
                } finally {
                    lock.unlock();
                }
            }
            if (error != null) {
                throw new InitializationFailed(error);
            }
            return server;
        }

        private void init() {
            if (System.getProperty("helidon.config.profile") == null
                && System.getProperty("config.profile") == null) {
                System.setProperty("helidon.config.profile", "test");
            }

            // lazy config source for test.server.port
            Services.add(ConfigSource.class, 10000D, (ConfigSource & LazyConfigSource) key -> {
                if ("test.server.port".equals(key)) {
                    return Optional.ofNullable(server)
                            .map(s -> ConfigNode.ValueNode.create(String.valueOf(s.port())));
                }
                return Optional.empty();
            });

            ServerTest annot = testClass.getAnnotation(ServerTest.class);
            if (annot == null) {
                throw new IllegalStateException(
                        "Test class %s is not annotated with %s"
                                .formatted(testClass, ServerTest.class));
            }

            if (annot.pinningDetection()) {
                pinningRecorder = PinningRecorder.create();
                pinningRecorder.record(Duration.ofMillis(annot.pinningThreshold()));
            }

            WebServerConfig.Builder builder = WebServer.builder();
            builder.config(Services.get(io.helidon.common.config.Config.class).get("server"));
            updateServerBuilder(builder);
            builder.host("localhost");

            extensions.forEach(it -> it.updateServerBuilder(builder));

            // port will be random
            builder.port(0)
                    .shutdownHook(false);

            setupFeatures(builder, testClass);
            setupServer(builder, testClass);
            addRouting(builder, testClass);

            server = builder
                    .serverContext(ctx)
                    .build()
                    .start();

            if (server.hasTls()) {
                uris.put(DEFAULT_SOCKET_NAME, URI.create("https://localhost:" + server.port() + "/"));
            } else {
                uris.put(DEFAULT_SOCKET_NAME, URI.create("http://localhost:" + server.port() + "/"));
            }
        }

        private static void updateServerBuilder(WebServerConfig.Builder builder) {
            Object svc = GlobalServiceRegistry.registry().get(WebServerService__ServiceDescriptor.INSTANCE)
                    .orElseThrow(() -> new IllegalStateException("Could not discover WebServerService in service registry"));

            // the service is package local
            Class<?> clazz = svc.getClass();
            try {
                Method method = clazz.getDeclaredMethod("updateServerBuilder", WebServerConfig.BuilderBase.class);
                method.setAccessible(true);
                method.invoke(svc, builder);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        private void addRouting(WebServerConfig.Builder builder, Class<?> testClass) {
            Map<String, ListenerConfig.Builder> listeners = new HashMap<>();
            Map<String, Router.Builder> routers = new HashMap<>();

            listeners.put(DEFAULT_SOCKET_NAME, ListenerConfig.builder().from(builder));

            withStaticMethods(testClass, SetUpRoute.class, (annot, method) -> {
                String socket = annot.value();
                ListenerConfig.Builder listener = listeners.computeIfAbsent(socket, it -> ListenerConfig.builder());
                Router.RouterBuilder<?> route = routers.computeIfAbsent(socket, it -> Router.builder());
                extensions.forEach(it -> it.updateListenerBuilder(socket, listener, route));
                handleParams(method, socket, builder, listener, route);
            });

            routers.forEach((socketName, routerBuilder) -> {
                if (DEFAULT_SOCKET_NAME.equals(socketName)) {
                    builder.addRoutings(routerBuilder.routings());
                } else {
                    listeners.computeIfAbsent(socketName, it -> ListenerConfig.builder())
                            .addRoutings(routerBuilder.routings());
                }
            });

            listeners.forEach((socketName, listenerBuilder) -> {
                if (DEFAULT_SOCKET_NAME.equals(socketName)) {
                    builder.from(listenerBuilder);
                } else {
                    ListenerConfig listenerConfig = builder.sockets().get(socketName);
                    if (listenerConfig == null) {
                        builder.putSocket(socketName, listenerBuilder.build());
                    } else {
                        builder.putSocket(socketName, ListenerConfig.builder(listenerConfig).from(listenerBuilder).build());
                    }
                }
            });
        }
    }

    private static final class InitializationFailed extends RuntimeException {
        private InitializationFailed(RuntimeException error) {
            super("Server initialization previously failed", error);
        }
    }
}
