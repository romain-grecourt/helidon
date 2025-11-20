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

import java.util.ArrayList;
import java.util.List;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.ServiceRegistryManager;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/**
 * Suite junit 5 extension.
 *
 * @deprecated this is a feature in progress of development, there may be backward incompatible changes done to it, so please
 *         use with care
 */
@Deprecated
public class SuiteExtension implements BeforeAllCallback, ParameterResolver {

    private static final String GLOBAL_CONTEXT_CLASSIFIER = "helidon-registry-static-context";
    private static final String GLOBAL_REGISTRY_CLASSIFIER = "helidon-registry";

    private SuiteDescriptor descriptor;

    /**
     * Creates an instance of suite junit 5 extension.
     */
    public SuiteExtension() {
        LogConfig.configureRuntime();
    }

    @Override
    public void beforeAll(ExtensionContext ctx) {
        var globalStore = ctx.getRoot().getStore(GLOBAL);
        var testClass = ctx.getRequiredTestClass();
        var suite = testClass.getAnnotation(TestSuite.Suite.class);
        if (suite == null) {
            throw new IllegalStateException(String.format(
                    "@Suite annotation was not found on %s class", testClass));
        }

        var providerClass = suite.value();
        var storeKey = providerClass.getName();
        descriptor = globalStore.get(storeKey, SuiteDescriptor.class);

        if (descriptor == null) {
            descriptor = SuiteDescriptor.create(suite, ctx);
            globalStore.put(storeKey, descriptor);

            var staticCtx = globalStore.get(GLOBAL_CONTEXT_CLASSIFIER, Context.class);
            if (staticCtx == null) {
                staticCtx = Context.builder()
                        .id("suite-" + providerClass.getName() + "-" + System.identityHashCode(providerClass))
                        .build();
                var registryManager = ServiceRegistryManager.create();
                staticCtx.register(GLOBAL_CONTEXT_CLASSIFIER, staticCtx);
                staticCtx.register(GLOBAL_REGISTRY_CLASSIFIER, registryManager.registry());
                globalStore.put(GLOBAL_CONTEXT_CLASSIFIER, staticCtx);

                var registryManagers = globalStore.get(RegistryManagers.class.getName(), RegistryManagers.class);
                if (registryManagers == null) {
                    registryManagers = new RegistryManagers();
                    globalStore.put(RegistryManagers.class.getName(), registryManagers);
                }
                registryManagers.add(registryManager);
            }

            Contexts.runInContext(staticCtx, descriptor::init);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return descriptor.supportsParameter(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return descriptor.resolveParameter(parameterContext.getParameter().getType());
    }

    private static final class RegistryManagers implements CloseableResource {

        private final List<ServiceRegistryManager> managers = new ArrayList<>();

        void add(ServiceRegistryManager manager) {
            managers.add(manager);
        }

        @Override
        public void close() {
            for (var manager : managers) {
                manager.shutdown();
            }
        }
    }
}
