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

package io.helidon.webserver.testing.junit5;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.common.context.Contexts;
import io.helidon.common.testing.virtualthreads.PinningRecorder;
import io.helidon.config.Config;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.Service;
import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerService__ServiceDescriptor;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension.ParamHandler;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import static io.helidon.webserver.WebServer.DEFAULT_SOCKET_NAME;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotation;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;

/**
 * {@link WebServerTestService} to support "in-process" tests.
 *
 * @see io.helidon.webserver.testing.junit5.ServerTest
 */
@Service.Singleton
final class WebServerTestServerService extends WebServerTestService<ServerJunitExtension> {

    private final Map<String, URI> uris = new ConcurrentHashMap<>();
    private final WebServer server;
    private PinningRecorder pinningRecorder;

    WebServerTestServerService(List<ServerJunitExtension> extensions,
                               List<WebServerRef> refs,
                               WebServerTestClass testClass,
                               Config config) {

        super(extensions, testClass);
        var annot = annotation(testClass.type(), ServerTest.class);
        if (annot.pinningDetection()) {
            pinningRecorder = PinningRecorder.create();
            pinningRecorder.record(Duration.ofMillis(annot.pinningThreshold()));
        }
        server = startServer(extensions, config, testClass);
        uris.put("@default", uri("@default"));
        for (var ref : refs) {
            ref.server(server);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        var paramType = pc.getParameter().getType();
        if (paramType.equals(URI.class)) {
            return true;
        }
        return super.supportsParameter(pc, ec);
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        var paramType = pc.getParameter().getType();
        if (paramType.equals(URI.class)) {
            var socketName = socketName(pc.getParameter());
            var uri = uris.computeIfAbsent(socketName, this::uri);
            if (uri == null) {
                throw new IllegalStateException("Socket not found: " + socketName);
            }
            return uri;
        }
        for (var ext : extensions()) {
            if (ext.supportsParameter(pc, ec)) {
                return ext.resolveParameter(pc, ec, paramType, server);
            }
        }
        throw new ParameterResolutionException(
                "Failed to resolve parameter of type " + paramType.getName());
    }

    @Override
    public void stop() {
        server.stop();
        super.stop();
        if (pinningRecorder != null) {
            pinningRecorder.close();
        }
    }

    private WebServer startServer(List<ServerJunitExtension> extensions, Config config, WebServerTestClass testClass) {
        var builder = WebServer.builder();
        builder.config(config.get("server"));
        updateServerBuilder(builder);
        builder.host("localhost");

        for (var e : extensions) {
            e.updateServerBuilder(builder);
        }

        // port will be random
        builder.port(0).shutdownHook(false);

        testClass.setupFeatures(builder);
        testClass.setupServer(builder);
        setupRouting(builder);

        var context = Contexts.context().orElseThrow();
        return builder.serverContext(context)
                .build()
                .start();
    }

    private URI uri(String socketName) {
        int port = server.port(socketName);
        if (port > 0) {
            if (server.hasTls()) {
                return URI.create("https://localhost:%d/".formatted(port));
            }
            return URI.create("http://localhost:%d/".formatted(port));
        } else {
            return null;
        }
    }

    private void setupRouting(WebServerConfig.Builder builder) {
        Map<String, ListenerConfig.Builder> listeners = new HashMap<>();
        Map<String, Router.Builder> routers = new HashMap<>();

        listeners.put(DEFAULT_SOCKET_NAME, ListenerConfig.builder().from(builder));

        var annotated = annotated(methods(testClass().type()));
        var elements = filterAnnotated(annotated, SetUpRoute.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                for (var a : e.annotations()) {
                    var socket = a.value();
                    var listener = listeners.computeIfAbsent(socket, it -> ListenerConfig.builder());
                    var route = routers.computeIfAbsent(socket, it -> Router.builder());
                    for (var it : extensions()) {
                        it.updateListenerBuilder(socket, listener, route);
                    }
                    handleParams(m, socket, builder, listener, route);
                }
            }
        }

        for (var entry : routers.entrySet()) {
            var key = entry.getKey();
            var router = entry.getValue();
            if (DEFAULT_SOCKET_NAME.equals(key)) {
                builder.addRoutings(router.routings());
            } else {
                listeners.computeIfAbsent(key, it -> ListenerConfig.builder())
                        .addRoutings(router.routings());
            }
        }

        for (var entry : listeners.entrySet()) {
            var socket = entry.getKey();
            var listener = entry.getValue();
            if (DEFAULT_SOCKET_NAME.equals(socket)) {
                builder.from(listener);
            } else {
                var config = builder.sockets().get(socket);
                if (config == null) {
                    builder.putSocket(socket, listener.build());
                } else {
                    builder.putSocket(socket, ListenerConfig.builder(config)
                            .from(listener)
                            .build());
                }
            }
        }
    }

    private void handleParams(Method method,
                              String socket,
                              WebServerConfig.Builder server,
                              ListenerConfig.Builder listener,
                              Router.RouterBuilder<?> router) {

        var handlers = new ArrayList<ParamHandler<?>>();
        var parameters = method.getParameters();
        for (var parameter : parameters) {
            var paramType = parameter.getType();
            boolean found = false;
            for (var ext : extensions()) {
                var handler = ext.setUpRouteParamHandler(paramType).orElse(null);
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
        var values = new Object[handlers.size()];
        for (int i = 0; i < handlers.size(); i++) {
            var handler = handlers.get(i);
            values[i] = handler.get(socket, server, listener, router);
        }

        invokeMethod(method, null, values);

        for (int i = 0; i < values.length; i++) {
            handleParam(handlers.get(i), socket, server, listener, router, values[i]);
        }
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

    private static String socketName(Parameter parameter) {
        var socket = parameter.getAnnotation(Socket.class);
        return socket != null ? socket.value() : WebServer.DEFAULT_SOCKET_NAME;
    }

    private static void updateServerBuilder(WebServerConfig.Builder builder) {
        try {
            var svc = GlobalServiceRegistry.registry()
                    .get(WebServerService__ServiceDescriptor.INSTANCE)
                    .orElseThrow();
            var method = svc.getClass().getDeclaredMethod("updateServerBuilder", WebServerConfig.BuilderBase.class);
            invokeMethod(method, svc, builder);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
