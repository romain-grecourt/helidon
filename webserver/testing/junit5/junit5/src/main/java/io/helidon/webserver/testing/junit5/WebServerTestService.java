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

import io.helidon.webserver.testing.junit5.spi.HelidonJunitExtension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.webserver.testing.junit5.ReflectionHelper.invokeMethod;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.methods;
import static io.helidon.webserver.testing.junit5.ReflectionHelper.requireStatic;

/**
 * WebServer test service.
 */
abstract class WebServerTestService<T extends HelidonJunitExtension> implements ParameterResolver {

    private final List<T> extensions;
    private final WebServerTestClass testClass;

    WebServerTestService(List<T> extensions, WebServerTestClass testClass) {
        this.extensions = extensions;
        this.testClass = testClass;
    }

    List<T> extensions() {
        return extensions;
    }

    WebServerTestClass testClass() {
        return testClass;
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) throws ParameterResolutionException {
        for (var ext : extensions()) {
            if (ext.supportsParameter(pc, ec)) {
                return true;
            }
        }
        return false;
    }

    void stop() {
        var methods = methods(testClass.type(), AfterStop.class);
        for (var e : methods) {
            if (e.element() instanceof Method method) {
                if (method.getParameterCount() > 0) {
                    throw new IllegalStateException(
                            "Method annotated with @AfterStop has parameters: " + method);
                }
                invokeMethod(requireStatic(method), method);
            }
        }
    }
}
