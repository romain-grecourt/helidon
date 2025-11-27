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
import java.util.ArrayList;
import java.util.List;

import io.helidon.config.Config;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.spi.ServerFeature;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension;
import io.helidon.webserver.testing.junit5.spi.DirectJunitExtension.ParamHandler;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;

/**
 * {@link WebServerTestService} to support "in-memory" tests.
 *
 * @see io.helidon.webserver.testing.junit5.RoutingTest
 */
@Service.Singleton
final class WebServerTestRoutingService extends WebServerTestService<DirectJunitExtension> {

    WebServerTestRoutingService(List<DirectJunitExtension> extensions,
                                WebServerTestClass testClass,
                                Config config) {

        super(extensions, testClass);
        var builder = WebServer.builder()
                .config(config.get("server"))
                .host("localhost")
                .port(0);

        testClass.setupFeatures(builder);
        testClass.setupServer(builder);

        var serverConfig = builder.buildPrototype();
        setUpRoute(serverConfig.features());
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        var paramType = pc.getParameter().getType();
        for (var ext : extensions()) {
            if (ext.supportsParameter(pc, ec)) {
                return ext.resolveParameter(pc, ec, paramType);
            }
        }
        throw new ParameterResolutionException("Failed to resolve parameter of type " + paramType.getName());
    }

    private void setUpRoute(List<ServerFeature> features) {
        var elements = methods(testClass().type(), SetUpRoute.class);
        for (var e : elements) {
            if (e.element() instanceof Method method) {
                for (var a : e.annotations()) {
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

                    var socket = a.value();
                    var values = new Object[handlers.size()];
                    for (int i = 0; i < handlers.size(); i++) {
                        values[i] = handlers.get(i).get(socket);
                    }

                    invokeMethod(method, null, values);

                    for (int i = 0; i < values.length; i++) {
                        handleParam(handlers.get(i), method, socket, values[i]);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void handleParam(ParamHandler<T> handler, Method method, String socket, Object value) {
        handler.handle(method, socket, (T) value);
    }
}
