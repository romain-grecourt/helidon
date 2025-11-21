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

package io.helidon.webserver.testing.junit5.websocket;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.webclient.websocket.WsClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension;
import io.helidon.webserver.websocket.WsRouting;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * A {@link DirectJunitExtension} that supports injection of WebSocket related types.
 */
public class WsDirectExtension implements DirectJunitExtension {
    private final Map<String, DirectWsClient> clients = new HashMap<>();

    @Override
    public void afterAll(ExtensionContext context) {
        clients.values().forEach(DirectWsClient::close);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        var paramType = parameterContext.getParameter().getType();
        return DirectWsClient.class.equals(paramType) || WsClient.class.equals(paramType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext, Class<?> parameterType) {
        var paramType = parameterContext.getParameter().getType();
        if (DirectWsClient.class.equals(paramType) || WsClient.class.equals(paramType)) {
            var socketName = socketName(parameterContext.getParameter());
            var directClient = clients.get(socketName);
            if (directClient == null) {
                if (WebServer.DEFAULT_SOCKET_NAME.equals(socketName)) {
                    throw new IllegalStateException("There is no WebSocket routing specified. Please add static method "
                                                            + "annotated with @SetUpRoute that accepts WebSocketRouting.Builder");
                } else {
                    throw new IllegalStateException(
                            ("There is no default routing specified for socket \"%s\".annotated with @SetUpRoute"
                             + " that accepts WebSocketRouting.Builder and add @Socket(\"%s\") annotation to the parameter")
                                    .formatted(socketName, socketName));
                }
            }
            return directClient;
        }

        throw new ParameterResolutionException("Parameter not supported by this extension: " + parameterType);
    }

    @Override
    public Optional<ParamHandler<?>> setUpRouteParamHandler(List<ServerFeature> features, Class<?> type) {
        if (WsRouting.Builder.class.equals(type)) {
            return Optional.of(new RoutingParamHandler(clients));
        }
        return Optional.empty();
    }

    private record RoutingParamHandler(Map<String, DirectWsClient> clients) implements ParamHandler<WsRouting.Builder> {

        @Override
        public WsRouting.Builder get(String socketName) {
            return WsRouting.builder();
        }

        @Override
        public void handle(Method method, String socketName, WsRouting.Builder value) {
            if (clients.putIfAbsent(socketName, DirectWsClient.create(value.build())) != null) {
                throw new IllegalStateException(
                        "Method %s defines WebSocket routing for socket \"%s\" that is already defined for class \"%s\"."
                                .formatted(method, socketName, method.getDeclaringClass().getName()));
            }
        }
    }
}
