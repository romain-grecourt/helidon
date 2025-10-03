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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.helidon.common.context.Context;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension.ParamHandler;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.webserver.testing.junit5.Junit5Util.withStaticMethods;

/**
 * JUnit5 extension to support Helidon WebServer in-memory unit tests.
 */
class HelidonRoutingJunitExtension extends JunitExtensionBase<DirectJunitExtension>
        implements AfterAllCallback, ParameterResolver {

    HelidonRoutingJunitExtension() {
        super(DirectJunitExtension.class, Set.of());
    }

    @Override
    @SuppressWarnings({"deprecation", "removal"})
    void init(Class<?> testClass, Context ctx) {
        RoutingTest testAnnot = testClass.getAnnotation(RoutingTest.class);
        if (testAnnot == null) {
            throw new IllegalStateException(
                    "Test class %s is not annotated with @RoutingTest"
                            .formatted(testClass));
        }

        var config = Services.get(io.helidon.common.config.Config.class);
        var builder = WebServer.builder()
                .config(config.get("server"))
                .host("localhost")
                .port(0);

        setupFeatures(builder, testClass);
        setupServer(builder, testClass);

        var server = builder.buildPrototype();
        withStaticMethods(testClass, SetUpRoute.class, (annot, method) -> {
            var socket = annot.value();
            handleParams(server.features(), method, socket);
        });
    }

    @Override
    Object resolve(ParameterContext pc, ExtensionContext ctx) {
        for (DirectJunitExtension extension : extensions()) {
            if (extension.supportsParameter(pc, ctx)) {
                init(ctx);
                return extension.resolveParameter(pc, ctx, pc.getParameter().getType());
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> void handleParam(ParamHandler<T> handler, Method method, String socket, Object value) {
        handler.handle(method, socket, (T) value);
    }

    private void handleParams(List<ServerFeature> features, Method method, String socket) {
        List<ParamHandler<?>> handlers = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            boolean found = false;
            Class<?> paramType = parameter.getType();
            for (DirectJunitExtension e : extensions()) {
                var handler = e.setUpRouteParamHandler(features, paramType).orElse(null);
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
            values[i] = handlers.get(i).get(socket);
        }

        try {
            method.setAccessible(true);
            method.invoke(null, values);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot invoke @SetUpRoute method", e);
        }

        for (int i = 0; i < values.length; i++) {
            handleParam(handlers.get(i), method, socket, values[i]);
        }
    }
}
