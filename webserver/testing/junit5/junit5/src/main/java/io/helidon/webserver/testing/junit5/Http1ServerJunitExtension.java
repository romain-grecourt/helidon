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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.common.testing.http.junit5.SocketHttpClient;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.testing.junit5.spi.ServerJunitExtension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * A {@link ServerJunitExtension} that supports HTTP/1.1 tests.
 */
public class Http1ServerJunitExtension implements ServerJunitExtension {
    private final Map<String, SocketHttpClient> socketHttpClients = new ConcurrentHashMap<>();
    private final Map<String, Http1Client> httpClients = new ConcurrentHashMap<>();
    private final Map<String, WebClient> webClients = new ConcurrentHashMap<>();

    /**
     * Required for {@link java.util.ServiceLoader}.
     *
     * @deprecated only for {@link java.util.ServiceLoader}
     */
    public Http1ServerJunitExtension() {
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        socketHttpClients.values().forEach(SocketHttpClient::disconnect);
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ctx)
            throws ParameterResolutionException {

        var paramType = pc.getParameter().getType();
        if (paramType.equals(Http1Client.class)) {
            return true;
        }
        if (paramType.equals(SocketHttpClient.class)) {
            return true;
        }
        return paramType.equals(WebClient.class);
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ctx, Class<?> paramType, WebServer server) {
        if (paramType.equals(SocketHttpClient.class)) {
            return socketHttpClients.computeIfAbsent(socketName(pc.getParameter()), it -> socketHttpClient(server, it));
        }
        if (paramType.equals(Http1Client.class)) {
            return httpClients.computeIfAbsent(socketName(pc.getParameter()), it -> httpClient(server, it));
        }
        if (paramType.equals(WebClient.class)) {
            return webClients.computeIfAbsent(socketName(pc.getParameter()), it -> webClient(server, it));
        }
        throw new ParameterResolutionException("Parameter of type %s not supported".formatted(paramType.getName()));
    }

    @Override
    public Optional<ParamHandler<?>> setUpRouteParamHandler(Class<?> type) {
        if (ListenerConfig.Builder.class.equals(type)) {
            return Optional.of(new ListenerConfigurationParamHandler());
        } else if (Router.RouterBuilder.class.equals(type)) {
            return Optional.of(new RouterParamHandler());
        } else if (HttpRules.class.equals(type) || HttpRouting.Builder.class.equals(type)) {
            return Optional.of(new RoutingParamHandler());
        }
        return Optional.empty();
    }

    private WebClient webClient(WebServer server, String socket) {
        return WebClient.builder()
                .baseUri("http://localhost:" + server.port(socket))
                .build();
    }

    private Http1Client httpClient(WebServer server, String socket) {
        return Http1Client.builder()
                .baseUri("http://localhost:" + server.port(socket))
                .build();
    }

    private SocketHttpClient socketHttpClient(WebServer server, String socketName) {
        return SocketHttpClient.create(server.port(socketName));
    }

    private static class RoutingParamHandler implements ParamHandler<HttpRouting.Builder> {
        @Override
        public HttpRouting.Builder get(String socket,
                                       WebServerConfig.Builder server,
                                       ListenerConfig.Builder listener,
                                       Router.RouterBuilder<?> router) {
            if (listener.routing().isEmpty()) {
                listener.routing(HttpRouting.builder());
            }
            return listener.routing().get();
        }

        @Override
        public void handle(String socket,
                           WebServerConfig.Builder server,
                           ListenerConfig.Builder listener,
                           Router.RouterBuilder<?> router,
                           HttpRouting.Builder value) {

            router.addRouting(value);
        }
    }

    private static class RouterParamHandler implements ParamHandler<Router.RouterBuilder<?>> {
        @Override
        public Router.RouterBuilder<?> get(String socket,
                                           WebServerConfig.Builder server,
                                           ListenerConfig.Builder listener,
                                           Router.RouterBuilder<?> router) {
            return router;
        }
    }

    private static class ListenerConfigurationParamHandler implements ParamHandler<ListenerConfig.Builder> {
        @Override
        public ListenerConfig.Builder get(String socket,
                                          WebServerConfig.Builder server,
                                          ListenerConfig.Builder listener,
                                          Router.RouterBuilder<?> router) {
            return listener;
        }
    }
}
