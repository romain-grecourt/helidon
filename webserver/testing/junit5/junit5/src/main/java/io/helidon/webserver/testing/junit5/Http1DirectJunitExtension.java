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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.Builder;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import static io.helidon.webserver.WebServer.DEFAULT_SOCKET_NAME;

/**
 * A {@link DirectJunitExtension} that supports HTTP/1.1 in-memory tests.
 */
public class Http1DirectJunitExtension implements DirectJunitExtension {
    private final Map<String, DirectClient> clients = new HashMap<>();
    private final Map<String, DirectWebClient> webClients = new HashMap<>();

    /**
     * Required for {@link java.util.ServiceLoader}.
     *
     * @deprecated only for {@link java.util.ServiceLoader}
     */
    public Http1DirectJunitExtension() {
    }

    @Override
    public void afterAll(ExtensionContext context) {
        clients.values().forEach(DirectClient::close);
        webClients.values().forEach(DirectWebClient::close);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clients.values().forEach(Http1DirectJunitExtension::reset);
        webClients.values().forEach(Http1DirectJunitExtension::reset);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        var paramType = parameterContext.getParameter().getType();
        if (DirectClient.class.equals(paramType)) {
            return true;
        }
        if (Http1Client.class.equals(paramType)) {
            return true;
        }
        return WebClient.class.equals(paramType);
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec, Class<?> paramType) {
        if (DirectClient.class.equals(paramType) || Http1Client.class.equals(paramType)) {
            var socketName = socketName(pc.getParameter());
            var directClient = clients.get(socketName);
            if (directClient == null) {
                if (DEFAULT_SOCKET_NAME.equals(socketName)) {
                    throw new IllegalStateException("There is no default routing specified. Please add static method "
                                                    + "annotated with @SetUpRoute that accepts HttpRouting.Builder,"
                                                    + " or HttpRules");
                } else {
                    throw new IllegalStateException((
                            "There is no routing specified for socket \"%s\". Please add static method annotated with"
                            + " @SetUpRoute that accepts HttpRouting.Builder,"
                            + " or HttpRules, and add @Socket(\"%s\") annotation to the parameter").formatted(
                            socketName, socketName));
                }
            }
            return directClient;
        }
        if (WebClient.class.equals(paramType)) {
            var socketName = socketName(pc.getParameter());
            var directClient = webClients.get(socketName);
            if (directClient == null) {
                if (DEFAULT_SOCKET_NAME.equals(socketName)) {
                    throw new IllegalStateException("There is no default routing specified. Please add static method "
                                                    + "annotated with @SetUpRoute that accepts HttpRouting.Builder,"
                                                    + " or HttpRules");
                } else {
                    throw new IllegalStateException((
                            "There is no default routing specified for socket \"%s\"."
                            + " Please add static method annotated with @SetUpRoute that accepts HttpRouting.Builder,"
                            + " or HttpRules, and add @Socket(\"%s\") annotation to the parameter").formatted(
                            socketName,
                            socketName));
                }
            }
            return directClient;
        }
        throw new ParameterResolutionException("Cannot resolve parameter: " + pc);
    }

    @Override
    public Optional<ParamHandler<?>> setUpRouteParamHandler(List<ServerFeature> features, Class<?> type) {
        if (HttpRouting.Builder.class.equals(type) || HttpRules.class.equals(type)) {
            return Optional.of(new RoutingParamHandler(clients, webClients, features));
        }
        return Optional.empty();
    }

    private static DirectClient reset(DirectClient client) {
        return client.clientTlsPrincipal(null)
                .clientTlsCertificates(null)
                .clientHost("helidon-unit")
                .clientPort(65000)
                .serverHost("helidon-unit-server")
                .serverPort(8080);
    }

    private static DirectWebClient reset(DirectWebClient client) {
        return client.clientTlsPrincipal(null)
                .clientTlsCertificates(null)
                .clientHost("helidon-unit")
                .clientPort(65000)
                .serverHost("helidon-unit-server")
                .serverPort(8080);
    }

    private record RoutingParamHandler(Map<String, DirectClient> clients,
                                       Map<String, DirectWebClient> webClients,
                                       List<ServerFeature> features)
            implements ParamHandler<HttpRouting.Builder> {

        @Override
        public HttpRouting.Builder get(String socketName) {
            return HttpRouting.builder();
        }

        @Override
        public void handle(Method method, String socket, HttpRouting.Builder value) {
            var routing = value.copy().build();
            routing.beforeStart();

            var featureContext = new DirectFeatureContext(socket, value);
            for (ServerFeature feature : features) {
                feature.setup(featureContext);
            }

            if (clients.putIfAbsent(socket, reset(new DirectClient(value))) != null
                || webClients.putIfAbsent(socket, reset(new DirectWebClient(value))) != null) {
                throw new IllegalStateException(
                        "Method %s defines HTTP routing for socket \"%s\" that is already defined for class %s.".formatted(
                                method,
                                socket,
                                method.getDeclaringClass().getName()));
            }
        }
    }

    private record DirectFeatureContext(String socket, HttpRouting.Builder routing)
            implements ServerFeature.ServerFeatureContext {

        @Override
        public WebServerConfig serverConfig() {
            return WebServerConfig.create();
        }

        @Override
        public Set<String> sockets() {
            return DEFAULT_SOCKET_NAME.equals(socket) ? Set.of() : Set.of(socket);
        }

        @Override
        public boolean socketExists(String socket) {
            return socket.equals(this.socket);
        }

        @Override
        public ServerFeature.SocketBuilders socket(String socket) {
            if (!socket.equals(this.socket)) {
                if (DEFAULT_SOCKET_NAME.equals(socket)) {
                    return defaultListener();
                }
                throw new NoSuchElementException("Socket %s is not defined".formatted(socket));
            }
            return new DirectSocketBuilders(routing);
        }

        ServerFeature.SocketBuilders defaultListener() {
            if (DEFAULT_SOCKET_NAME.equals(socket)) {
                return new DirectSocketBuilders(routing);
            }
            return new DirectSocketBuilders(HttpRouting.builder());
        }
    }

    private record DirectSocketBuilders(HttpRouting.Builder routing) implements ServerFeature.SocketBuilders {

        @Override
        public ListenerConfig listener() {
            return ListenerConfig.create();
        }

        @Override
        public HttpRouting.Builder httpRouting() {
            return routing;
        }

        @Override
        public ServerFeature.RoutingBuilders routingBuilders() {
            return new ServerFeature.RoutingBuilders() {
                @Override
                public boolean hasRouting(Class<?> builderType) {
                    return false;
                }

                @Override
                public <T extends Builder<T, ?>> T routingBuilder(Class<T> builderType) {
                    if (builderType == HttpRouting.Builder.class) {
                        return builderType.cast(routing);
                    }
                    throw new NoSuchElementException("Routing not available for type: " + builderType);
                }
            };
        }
    }
}
