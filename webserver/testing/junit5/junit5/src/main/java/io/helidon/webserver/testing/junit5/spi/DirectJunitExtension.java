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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import io.helidon.webserver.spi.ServerFeature;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * Base contract for Helidon JUnit extensions that support in-memory tests.
 */
public interface DirectJunitExtension extends HelidonJunitExtension {
    /**
     * Resolve a parameter.
     *
     * @param parameterContext JUnit parameter context
     * @param extensionContext JUnit extension context
     * @param parameterType    type of parameter
     * @return instance of the parameter
     * @throws ParameterResolutionException in case parameter cannot be resolved
     */
    default Object resolveParameter(ParameterContext parameterContext,
                                    ExtensionContext extensionContext,
                                    Class<?> parameterType) {
        throw new ParameterResolutionException("Cannot resolve parameter: " + parameterContext);
    }

    /**
     * Set up the parameter handler.
     *
     * @param features features
     * @param type     parameter type
     * @return optional parameter handler
     */
    default Optional<ParamHandler<?>> setUpRouteParamHandler(List<ServerFeature> features, Class<?> type) {
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
         * @return a new parameter instance
         */
        T get(String socketName);

        /**
         * Process the parameter instance post updates.
         *
         * @param method     update method
         * @param socketName socket name
         * @param value      parameter instance
         */
        default void handle(Method method, String socketName, T value) {
        }
    }
}
