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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.common.context.Context;
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import static io.helidon.webserver.WebServer.DEFAULT_SOCKET_NAME;
import static io.helidon.webserver.testing.junit5.Junit5Util.socketName;
import static io.helidon.webserver.testing.junit5.Junit5Util.withStaticMethods;

/**
 * JUnit5 extension to support Helidon WebServer in tests.
 */
class HelidonServerJunitExtension extends JunitExtensionBase<ServerJunitExtension> {

    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(WebServer.class, URI.class);

    private final Map<String, URI> uris = new ConcurrentHashMap<>();
    private WebServer server;
    private PinningRecorder pinningRecorder;

    HelidonServerJunitExtension() {
        super(ServerJunitExtension.class, SUPPORTED_TYPES);
    }

    @Override
    @SuppressWarnings({"removal", "deprecation"})
    void init(Class<?> testClass, Context ctx) {
        // lazy config source for test.server.port
        Services.add(ConfigSource.class, 10000D, (ConfigSource & LazyConfigSource) key -> {
            if ("test.server.port".equals(key)) {
                return Optional.ofNullable(server)
                        .map(s -> ConfigNode.ValueNode.create(String.valueOf(s.port())));
            }
            return Optional.empty();
        });

        var annot = testClass.getAnnotation(ServerTest.class);
        if (annot == null) {
            throw new IllegalStateException(
                    "Test class %s is not annotated with @ServerTest"
                            .formatted(testClass));
        }

        if (annot.pinningDetection()) {
            pinningRecorder = PinningRecorder.create();
            pinningRecorder.record(Duration.ofMillis(annot.pinningThreshold()));
        }

        var builder = WebServer.builder();
        var config = Services.get(io.helidon.common.config.Config.class);
        builder.config(config.get("server"));
        updateServerBuilder(builder);
        builder.host("localhost");

        extensions().forEach(it -> it.updateServerBuilder(builder));

        // port will be random
        builder.port(0).shutdownHook(false);

        setupFeatures(builder, testClass);
        setupServer(builder, testClass);
        setupRouting(builder, testClass);

        server = builder
                .serverContext(ctx)
                .build()
                .start();

        if (server.hasTls()) {
            uris.put(DEFAULT_SOCKET_NAME, URI.create("https://localhost:%d/".formatted(server.port())));
        } else {
            uris.put(DEFAULT_SOCKET_NAME, URI.create("http://localhost:%d/".formatted(server.port())));
        }
    }

    @Override
    public Object resolve(ParameterContext pc, ExtensionContext ctx) {
        var paramType = pc.getParameter().getType();
        if (paramType.equals(WebServer.class)) {
            init(ctx);
            return server;
        }
        if (paramType.equals(URI.class)) {
            init(ctx);
            var socketName = socketName(pc.getParameter());
            var uri = uris.computeIfAbsent(socketName, it -> {
                int port = server.port(it);
                if (port == -1) {
                    return null;
                }
                if (server.hasTls()) {
                    return URI.create("https://localhost:%d/".formatted(port));
                }
                return URI.create("http://localhost:%d/".formatted(port));
            });
            if (uri == null) {
                throw new IllegalStateException("Socket not found: " + socketName);
            }
            return uri;
        }
        for (ServerJunitExtension e : extensions()) {
            if (e.supportsParameter(pc, ctx)) {
                init(ctx);
                return e.resolveParameter(pc, ctx, paramType, server);
            }
        }
        return null;
    }

    @Override
    void close(Class<?> testClass) {
        if (server != null) {
            server.stop();
        }
        super.close(testClass);
        if (pinningRecorder != null) {
            pinningRecorder.close();
        }
    }

    private void setupRouting(WebServerConfig.Builder builder, Class<?> testClass) {
        Map<String, ListenerConfig.Builder> listeners = new HashMap<>();
        Map<String, Router.Builder> routers = new HashMap<>();

        listeners.put(DEFAULT_SOCKET_NAME, ListenerConfig.builder().from(builder));

        withStaticMethods(testClass, SetUpRoute.class, (annot, method) -> {
            var socket = annot.value();
            var listener = listeners.computeIfAbsent(socket, it -> ListenerConfig.builder());
            var route = routers.computeIfAbsent(socket, it -> Router.builder());
            extensions().forEach(it -> it.updateListenerBuilder(socket, listener, route));
            handleParams(method, socket, builder, listener, route);
        });

        routers.forEach((socket, router) -> {
            if (DEFAULT_SOCKET_NAME.equals(socket)) {
                builder.addRoutings(router.routings());
            } else {
                listeners.computeIfAbsent(socket, it -> ListenerConfig.builder())
                        .addRoutings(router.routings());
            }
        });

        listeners.forEach((socket, listener) -> {
            if (DEFAULT_SOCKET_NAME.equals(socket)) {
                builder.from(listener);
            } else {
                ListenerConfig config = builder.sockets().get(socket);
                if (config == null) {
                    builder.putSocket(socket, listener.build());
                } else {
                    builder.putSocket(socket, ListenerConfig.builder(config)
                            .from(listener)
                            .build());
                }
            }
        });
    }

    private void handleParams(Method method,
                              String socket,
                              WebServerConfig.Builder server,
                              ListenerConfig.Builder listener,
                              Router.RouterBuilder<?> router) {

        List<ParamHandler<?>> handlers = new ArrayList<>();
        var parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            var paramType = parameter.getType();
            boolean found = false;
            for (ServerJunitExtension e : extensions()) {
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

    @SuppressWarnings("unchecked")
    private <T> void handleParam(ParamHandler<T> handler,
                                 String socket,
                                 WebServerConfig.Builder server,
                                 ListenerConfig.Builder listener,
                                 Router.RouterBuilder<?> router,
                                 Object value) {

        handler.handle(socket, server, listener, router, (T) value);
    }

    private void updateServerBuilder(WebServerConfig.Builder builder) {
        try {
            var svc = GlobalServiceRegistry.registry()
                    .get(WebServerService__ServiceDescriptor.INSTANCE)
                    .orElseThrow();
            var method = svc.getClass().getDeclaredMethod("updateServerBuilder", WebServerConfig.BuilderBase.class);
            method.setAccessible(true); // the service is package local
            method.invoke(svc, builder);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
