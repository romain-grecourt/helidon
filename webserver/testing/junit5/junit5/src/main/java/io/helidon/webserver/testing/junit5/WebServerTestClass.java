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
import java.util.List;

import io.helidon.common.GenericType;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.spi.ServerFeature;

import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.requireStatic;

/**
 * WebServer test class.
 *
 * @param type test class
 */
@Service.Singleton
record WebServerTestClass(Class<?> type) {

    private static final GenericType<List<ServerFeature>> FEATURES_TYPE = new GenericType<>() {
    };

    /**
     * Set up the {@link io.helidon.webserver.WebServerConfig}.
     *
     * @param builder webserver builder
     */
    void setupServer(WebServerConfig.Builder builder) {
        var annotated = annotated(methods(type));
        var elements = filterAnnotated(annotated, SetUpServer.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                var paramTypes = m.getParameterTypes();
                if (paramTypes.length != 1) {
                    throw new IllegalArgumentException(
                            "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                    m, WebServerConfig.Builder.class.getCanonicalName()));
                }
                if (!paramTypes[0].equals(WebServerConfig.Builder.class)) {
                    throw new IllegalArgumentException(
                            "Method %s annotated with @SetUpServer must have one parameter: %s".formatted(
                                    m, WebServerConfig.Builder.class.getCanonicalName()));
                }
                invokeMethod(requireStatic(m), null, builder);
            }
        }
    }

    /**
     * Set up the {@link io.helidon.webserver.spi.ServerFeature}.
     *
     * @param builder webserver builder
     */
    void setupFeatures(WebServerConfig.Builder builder) {
        var annotated = annotated(methods(type));
        var elements = filterAnnotated(annotated, SetUpFeatures.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                for (var a : e.annotations()) {
                    if (!a.value()) {
                        builder.featuresDiscoverServices(false);
                    }
                    var parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length > 0) {
                        throw new IllegalArgumentException(
                                "Method %s annotated with @SetUpFeatures has parameters".formatted(
                                        m));
                    }

                    try {
                        var result = invokeMethod(FEATURES_TYPE, requireStatic(m), null);
                        for (var feature : result) {
                            builder.addFeature(feature);
                        }
                    } catch (ClassCastException ex) {
                        throw new IllegalArgumentException(
                                "Method %s annotated with @SetUpFeatures must return %s".formatted(
                                        m, FEATURES_TYPE), ex);
                    }
                }
            }
        }
    }
}
