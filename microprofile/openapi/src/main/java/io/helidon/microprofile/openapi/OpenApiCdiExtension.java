/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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
package io.helidon.microprofile.openapi;

import java.util.HashSet;
import java.util.Set;

import io.helidon.config.Config;
import io.helidon.microprofile.servicecommon.HelidonRestCdiExtension;
import io.helidon.openapi.OpenApiFeature;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import org.eclipse.microprofile.config.ConfigProvider;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_AFTER;

/**
 * Portable extension to allow construction of a Jandex index (to pass to
 * SmallRye OpenAPI) from CDI if no {@code META-INF/jandex.idx} file exists on
 * the class path.
 */
public class OpenApiCdiExtension extends HelidonRestCdiExtension<OpenApiFeature> {

    private static final System.Logger LOGGER = System.getLogger(OpenApiCdiExtension.class.getName());

    private final Set<Class<?>> annotatedTypes = new HashSet<>();

    /**
     * Creates a new instance.
     */
    public OpenApiCdiExtension() {
        super(LOGGER, OpenApiCdiExtension::createFeature, "openapi");
    }

    @Override
    protected void processManagedBean(ProcessManagedBean<?> processManagedBean) {
        // SmallRye handles annotation processing. We have this method because the abstract superclass requires it.
    }

    // Must run after the server has created the Application instances.
    void buildModel(@Observes @Priority(PLATFORM_AFTER + 100 + 10) @Initialized(ApplicationScoped.class) Object event) {
        serviceSupport().initialize();
    }

    // For testing
    OpenApiFeature feature() {
        return serviceSupport();
    }

    /**
     * Get the annotated types.
     *
     * @return annotated types
     */
    Set<Class<?>> annotatedTypes() {
        return annotatedTypes;
    }

    /**
     * Records each type that is annotated.
     *
     * @param <X>   annotated type
     * @param event {@code ProcessAnnotatedType} event
     */
    private <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event) {
        annotatedTypes.add(event.getAnnotatedType().getJavaClass());
    }

    private static OpenApiFeature createFeature(Config config) {
        return OpenApiFeature.builder()
                .config(config)
                .manager(new MpOpenApiManager(ConfigProvider.getConfig()))
                .build();
    }
}
