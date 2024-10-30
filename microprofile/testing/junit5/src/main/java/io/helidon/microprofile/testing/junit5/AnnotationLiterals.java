/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.microprofile.testing.junit5;

import java.io.Serial;
import java.lang.annotation.Annotation;

import io.helidon.microprofile.server.JaxRsCdiExtension;
import io.helidon.microprofile.server.ServerCdiExtension;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.AnnotationLiteral;
import org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider;

/**
 * {@link AnnotationLiteral} instances.
 */
@SuppressWarnings("ALL")
class AnnotationLiterals {

    private AnnotationLiterals() {
        // cannot be instanciated
    }

    /**
     * Literal of {@link org.glassfish.jersey.weld.se.WeldRequestScope}.
     */
    static final WeldRequestScopeLiteral WELD_REQUEST_SCOPE = new WeldRequestScopeLiteral();

    /**
     * Literal of {@link org.glassfish.jersey.ext.cdi1x.internal.ProcessAllAnnotatedTypes}.
     */
    static final ProcessAllAnnotatedTypesLiteral PROCESS_ALL_ANNOTATED_TYPES = new ProcessAllAnnotatedTypesLiteral();

    /**
     * Literal of {@code @AddExtension(ServerCdiExtension.class)}.
     */
    static final ServerCdiExtensionLiteral SERVER_CDI_EXTENSION = new ServerCdiExtensionLiteral();

    /**
     * Literal of {@code @AddExtension(JaxRsCdiExtension.class)}.
     */
    static final JaxRsCdiExtensionLiteral JAX_RS_CDI_EXTENSION = new JaxRsCdiExtensionLiteral();

    /**
     * Literal of {@code @AddExtension(CdiComponentProvider.class)}.
     */
    static final CdiComponentProviderLiteral CDI_COMPONENT_PROVIDER = new CdiComponentProviderLiteral();

    private static final class WeldRequestScopeLiteral extends AnnotationLiteral<AddBean> implements AddBean {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> value() {
            return org.glassfish.jersey.weld.se.WeldRequestScope.class;
        }

        @Override
        public Class<? extends Annotation> scope() {
            return RequestScoped.class;
        }
    }

    private static final class ProcessAllAnnotatedTypesLiteral extends AnnotationLiteral<AddExtension> implements AddExtension {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Class<? extends Extension> value() {
            return org.glassfish.jersey.ext.cdi1x.internal.ProcessAllAnnotatedTypes.class;
        }
    }

    private static final class ServerCdiExtensionLiteral extends AnnotationLiteral<AddExtension> implements AddExtension {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Class<? extends Extension> value() {
            return ServerCdiExtension.class;
        }
    }

    private static final class JaxRsCdiExtensionLiteral extends AnnotationLiteral<AddExtension> implements AddExtension {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Class<? extends Extension> value() {
            return JaxRsCdiExtension.class;
        }
    }

    private static final class CdiComponentProviderLiteral extends AnnotationLiteral<AddExtension> implements AddExtension {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Class<? extends Extension> value() {
            return CdiComponentProvider.class;
        }
    }
}
