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

package io.helidon.webserver.testing.junit5.spi;

import java.util.Optional;

import io.helidon.webserver.ListenerConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * Base contract for Helidon JUnit extensions that support in-process testing.
 */
public interface ServerJunitExtension extends HelidonJunitExtension {
    /**
     * Update {@link WebServer} builder.
     *
     * @param builder builder to update, will be used to build server instance
     */
    @SuppressWarnings("unused")
    default void updateServerBuilder(WebServerConfig.Builder builder) {
        // no-op
    }

    /**
     * Update {@link ListenerConfig} builder.
     *
     * @param socketName      name of the socket
     * @param listenerBuilder listener builder
     * @param routerBuilder   router builder
     */
    @SuppressWarnings("unused")
    default void updateListenerBuilder(String socketName,
                                       ListenerConfig.Builder listenerBuilder,
                                       Router.RouterBuilder<?> routerBuilder) {
        // no-op
    }

    /**
     * Resolve a parameter.
     *
     * @param parameterContext JUnit parameter context
     * @param extensionContext JUnit extension context
     * @param parameterType    type of parameter
     * @param server           webserver instance
     * @return instance of the expected type
     */
    default Object resolveParameter(ParameterContext parameterContext,
                                    ExtensionContext extensionContext,
                                    Class<?> parameterType,
                                    WebServer server) {
        throw new ParameterResolutionException("This parameter cannot be resolved: " + parameterContext);
    }

    /**
     * Set up the parameter handler.
     *
     * @param type     parameter type
     * @return optional parameter handler
     */
    default Optional<ParamHandler<?>> setUpRouteParamHandler(Class<?> type) {
        return Optional.empty();
    }

    /**
     * Parameter handler.
     *
     * @param <T> type of the parameter
     */
    interface ParamHandler<T> {
        /**
         * Get the initial parameter instance.
         *
         * @param socketName socket name
         * @param serverBuilder   server builder
         * @param listenerBuilder listener builder
         * @param routerBuilder   router builder
         * @return a new parameter instance
         */
        T get(String socketName,
              WebServerConfig.Builder serverBuilder,
              ListenerConfig.Builder listenerBuilder,
              Router.RouterBuilder<?> routerBuilder);

        /**
         * Process the parameter instance post updates.
         *
         * @param socketName      socket name
         * @param serverBuilder   server builder
         * @param listenerBuilder listener builder
         * @param routerBuilder   router builder
         * @param value           parameter instance
         */
        default void handle(String socketName,
                            WebServerConfig.Builder serverBuilder,
                            ListenerConfig.Builder listenerBuilder,
                            Router.RouterBuilder<?> routerBuilder,
                            T value) {
        }
    }
}
