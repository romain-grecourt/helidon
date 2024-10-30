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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

@SuppressWarnings("ALL")
class AddBeansExtension implements Extension {

    private static final Map<Class<? extends Annotation>, Annotation> BEAN_DEFINING = Map.of(
            ApplicationScoped.class, ApplicationScoped.Literal.INSTANCE,
            Singleton.class, ApplicationScoped.Literal.INSTANCE,
            RequestScoped.class, RequestScoped.Literal.INSTANCE,
            Dependent.class, Dependent.Literal.INSTANCE);

    private final Class<?> testClass;
    private final List<AddBean> addBeans;
    private final HashMap<String, Annotation> socketAnnotations = new HashMap<>();

    AddBeansExtension(Class<?> testClass, List<AddBean> addBeans) {
        this.testClass = testClass;
        this.addBeans = addBeans;
    }

    void processSocketInjectionPoints(@Observes ProcessInjectionPoint<?, WebTarget> event) {
        for (Annotation qualifier : event.getInjectionPoint().getQualifiers()) {
            if (qualifier.annotationType().equals(Socket.class)) {
                String value = ((Socket) qualifier).value();
                socketAnnotations.put(value, qualifier);
                break;
            }
        }
    }

    void registerOtherBeans(@Observes AfterBeanDiscovery event) {
        Client client = ClientBuilder.newClient();

        // register for all named Ports
        socketAnnotations.forEach((namedPort, qualifier) -> event
                .addBean()
                .addType(WebTarget.class)
                .scope(ApplicationScoped.class)
                .qualifiers(qualifier)
                .createWith(context -> webTarget(client, namedPort)));

        event.addBean()
                .addType(WebTarget.class)
                .scope(ApplicationScoped.class)
                .createWith(context -> webTarget(client, "@default"));

    }

    void registerAddedBeans(@Observes BeforeBeanDiscovery event) {
        event.addAnnotatedType(testClass, "junit-" + testClass.getName())
                .add(ApplicationScoped.Literal.INSTANCE);

        for (AddBean addBean : addBeans) {
            Class<? extends Annotation> scopeType = addBean.scope();
            Annotation scope = BEAN_DEFINING.get(scopeType);
            if (scope == null) {
                throw new IllegalStateException(
                        "Only on of " + BEAN_DEFINING.keySet() + " scopes are allowed in tests. Scope "
                        + scopeType.getName() + " is not allowed for bean " + addBean.value().getName());
            }
            String id = "junit-" + addBean.value().getName();
            AnnotatedTypeConfigurator<?> configurator = event.addAnnotatedType(addBean.value(), id);
            if (!AnnotationFinder.hasAny(addBean.value(), BEAN_DEFINING.keySet())) {
                configurator.add(scope);
            }
        }
    }

    private static WebTarget webTarget(Client client, String namedPort) {
        try {
            Class<? extends Extension> extClass = (Class<? extends Extension>) Class
                    .forName("io.helidon.microprofile.server.ServerCdiExtension");
            Extension extension = CDI.current().getBeanManager().getExtension(extClass);
            Method m = extension.getClass().getMethod("port", String.class);
            Object port = m.invoke(extension, new Object[] {namedPort});
            String uri = "http://localhost:" + port;
            return client.target(uri);
        } catch (ReflectiveOperationException e) {
            return client.target("http://localhost:7001");
        }
    }
}
