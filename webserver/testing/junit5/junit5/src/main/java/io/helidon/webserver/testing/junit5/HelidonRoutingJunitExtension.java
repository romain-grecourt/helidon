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

import java.lang.reflect.Method;
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
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.webserver.testing.junit5.ReflectionHelper.annotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotated;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.filterAnnotations;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;

/**
 * JUnit5 extension to support Helidon WebServer in-memory unit tests.
 * @see io.helidon.webserver.testing.junit5.RoutingTest
 */
class HelidonRoutingJunitExtension extends HelidonJunitExtensionBase<DirectJunitExtension>
        implements AfterAllCallback, ParameterResolver {

    HelidonRoutingJunitExtension() {
        super(DirectJunitExtension.class, Set.of());
    }

    @Override
    @SuppressWarnings({"removal", "deprecation"})
    protected void initClass(ExtensionContext ctx, Context staticContext) {
        var testClass = ctx.getRequiredTestClass();
        filterAnnotations(annotated(testClass), RoutingTest.class).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Test class %s is not annotated with @RoutingTest"
                                .formatted(testClass)));

        var config = Services.get(io.helidon.common.config.Config.class);
        var builder = WebServer.builder()
                .config(config.get("server"))
                .host("localhost")
                .port(0);

        setupFeatures(builder, testClass);
        setupServer(builder, testClass);

        var server = builder.buildPrototype();
        var annotated = annotated(methods(testClass));
        var elements = filterAnnotated(annotated, SetUpRoute.class);
        for (var e : elements) {
            if (e.element() instanceof Method m) {
                for (var a : e.annotations()) {
                    handleParams(server.features(), m, a.value());
                }
            }
        }
    }

    @Override
    protected Object resolve(ParameterContext pc, ExtensionContext ctx) {
        var paramType = pc.getParameter().getType();
        for (var ext : extensions()) {
            if (ext.supportsParameter(pc, ctx)) {
                return ext.resolveParameter(pc, ctx, paramType);
            }
        }
        throw new ParameterResolutionException(
                "Failed to resolve parameter of type " + paramType.getName());
    }

    private void handleParams(List<ServerFeature> features, Method method, String socket) {
        List<ParamHandler<?>> handlers = new ArrayList<>();
        for (var parameter : method.getParameters()) {
            boolean found = false;
            Class<?> paramType = parameter.getType();
            for (var ext : extensions()) {
                var handler = ext.setUpRouteParamHandler(features, paramType).orElse(null);
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

        invokeMethod(method, null, values);

        for (int i = 0; i < values.length; i++) {
            handleParam(handlers.get(i), method, socket, values[i]);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void handleParam(ParamHandler<T> handler, Method method, String socket, Object value) {
        handler.handle(method, socket, (T) value);
    }
}
