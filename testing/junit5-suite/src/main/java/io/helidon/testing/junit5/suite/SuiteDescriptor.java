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
package io.helidon.testing.junit5.suite;

import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import io.helidon.testing.junit5.suite.spi.SuiteProvider;

import org.junit.jupiter.api.extension.ExtensionContext;

@SuppressWarnings("deprecation")
class SuiteDescriptor implements ExtensionContext.Store.CloseableResource {

    private static final System.Logger LOGGER = System.getLogger(SuiteDescriptor.class.getName());
    private final SuiteProvider provider;
    private final Storage storage;

    private SuiteDescriptor(TestSuite.Suite suite, SuiteProvider provider, ExtensionContext ctx) {
        var ns = ExtensionContext.Namespace.create(suite.value().getName());
        this.storage = new StorageImpl(ctx.getRoot().getStore(ns));
        this.provider = provider;
    }

    static SuiteDescriptor create(TestSuite.Suite suite, ExtensionContext context) {
        try {
            var ctor = suite.value().getConstructor();
            ctor.newInstance();
            return new SuiteDescriptor(suite, ctor.newInstance(), context);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        LOGGER.log(Level.TRACE, "Closing suite {0}", provider.getClass());
        for (Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(TestSuite.AfterSuite.class)) {
                invoke(method);
            }
        }
    }

    boolean supportsParameter(Type type) {
        return (provider instanceof SuiteResolver resolver && resolver.supportsParameter(type))
               || (provider instanceof SuiteStorage && Storage.class.isAssignableFrom((Class<?>) type));
    }

    Object resolveParameter(Type type) {
        if (provider instanceof SuiteResolver suiteResolver && suiteResolver.supportsParameter(type)) {
            return suiteResolver.resolveParameter(type);
        } else if (provider instanceof SuiteStorage && Storage.class.isAssignableFrom((Class<?>) type)) {
            return storage;
        }
        throw new IllegalArgumentException(String.format(
                "Cannot resolve parameter Type %s", type.getTypeName()));
    }

    void init() {
        LOGGER.log(Level.TRACE, "Initializing suite {0}", provider.getClass());
        for (Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(TestSuite.BeforeSuite.class)) {
                invoke(method);
            }
        }
    }

    private void invoke(Method method) {
        Type[] types = method.getGenericParameterTypes();
        int count = method.getParameterCount();
        Object[] parameters = new Object[count];
        for (int i = 0; i < count; i++) {
            parameters[i] = resolve(types[i]);
        }
        invoke(method, parameters);
    }

    private Object resolve(Type type) {
        if (supportsParameter(type)) {
            return resolveParameter(type);
        } else {
            throw new IllegalArgumentException("Cannot resolve parameter Type " + type.getTypeName());
        }
    }

    private void invoke(Method method, Object[] parameters) {
        try {
            method.invoke(provider, parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format(
                    "Could not invoke %s method %s",
                    provider.getClass().getSimpleName(),
                    method.getName()),
                    e);
        }
    }
}
