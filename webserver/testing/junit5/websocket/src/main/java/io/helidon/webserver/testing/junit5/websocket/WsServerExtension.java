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

import java.util.Optional;

import io.helidon.webclient.websocket.WsClient;
import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension;
import io.helidon.webserver.websocket.WsRouting;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * A {@link ServerJunitExtension} that supports WebSocket tests.
 */
public class WsServerExtension implements ServerJunitExtension {

    /**
     * Required for {@link java.util.ServiceLoader}.
     *
     * @deprecated only for {@link java.util.ServiceLoader}
     */
    public WsServerExtension() {
    }

    @Override
    public Optional<ParamHandler<?>> setUpRouteParamHandler(Class<?> type) {
        if (WsRouting.Builder.class.equals(type)) {
            return Optional.of(new RoutingParamHandler());
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return WsClient.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext,
                                   Class<?> parameterType,
                                   WebServer server) {

        var socketName = socketName(parameterContext.getParameter());
        if (WsClient.class.equals(parameterType)) {
            return WsClient.builder()
                    .baseUri("ws://localhost:" + server.port(socketName))
                    .build();
        }
        throw new ParameterResolutionException("WebSocket extension only supports WsClient parameter type");
    }

    private static final class RoutingParamHandler implements ParamHandler<WsRouting.Builder> {
        @Override
        public WsRouting.Builder get(String socketName,
                                     WebServerConfig.Builder serverBuilder,
                                     ListenerConfig.Builder listenerBuilder,
                                     Router.RouterBuilder<?> routerBuilder) {
            return WsRouting.builder();
        }

        @Override
        public void handle(String socketName,
                           WebServerConfig.Builder serverBuilder,
                           ListenerConfig.Builder listenerBuilder,
                           Router.RouterBuilder<?> routerBuilder,
                           WsRouting.Builder value) {
            routerBuilder.addRouting(value);
        }
    }
}
