/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.config.mp.MpConfigSources;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static io.helidon.microprofile.testing.junit5.ReflectionHelper.invoke;
import static io.helidon.microprofile.testing.junit5.ReflectionHelper.isStatic;
import static io.helidon.microprofile.testing.junit5.ReflectionHelper.visitMethods;

/**
 * Junit5 extension to support Helidon CDI container in tests.
 */
class HelidonJunitExtension implements BeforeAllCallback,
                                       AfterAllCallback,
                                       BeforeEachCallback,
                                       AfterEachCallback,
                                       InvocationInterceptor,
                                       ParameterResolver {

    private static final Set<Class<? extends Annotation>> TEST_ANNOTATIONS = Set.of(
            AddBean.class,
            AddConfig.class,
            AddConfigBlock.class,
            AddExtension.class,
            AddJaxRs.class,
            Configuration.class);

    private final List<AddExtension> classLevelExtensions = new ArrayList<>();
    private final List<AddBean> classLevelBeans = new ArrayList<>();
    private final ConfigMeta classLevelConfigMeta = new ConfigMeta();
    private boolean classLevelDisableDiscovery = false;
    private boolean resetPerTest;
    private Class<?> testClass;
    private ConfigProviderResolver configProviderResolver;
    private Config config;
    private SeContainer container;

    @Override
    public void beforeAll(ExtensionContext context) {
        configProviderResolver = ConfigProviderResolver.instance();
        testClass = context.getRequiredTestClass();
        AnnotationFinder finder = new AnnotationFinder(testClass, TEST_ANNOTATIONS);

        finder.ifPresent(HelidonTest.class, a -> resetPerTest = a.resetPerTest());
        finder.ifPresent(DisableDiscovery.class, a -> classLevelDisableDiscovery = a.value());

        finder.forEach(AddExtensions.class, AddExtensions::value, classLevelExtensions::add);
        finder.forEach(AddBeans.class, AddBeans::value, classLevelBeans::add);
        finder.forEach(AddConfigs.class, AddConfigs::value, classLevelConfigMeta::config);
        finder.forEach(AddExtension.class, classLevelExtensions::add);
        finder.forEach(AddBean.class, classLevelBeans::add);
        finder.forEach(AddConfig.class, classLevelConfigMeta::config);
        finder.ifPresent(Configuration.class, classLevelConfigMeta::config);
        finder.ifPresent(AddConfigBlock.class, classLevelConfigMeta::config);

        visitMethods(testClass, m -> isStatic(m, AddConfigSource.class), classLevelConfigMeta::config);

        if (resetPerTest) {
            validatePerTest();
            return;
        }
        validatePerClass();

        if (finder.isPresent(AddJaxRs.class)) {
            classLevelExtensions.add(AnnotationLiterals.PROCESS_ALL_ANNOTATED_TYPES);
            classLevelExtensions.add(AnnotationLiterals.SERVER_CDI_EXTENSION);
            classLevelExtensions.add(AnnotationLiterals.JAX_RS_CDI_EXTENSION);
            classLevelExtensions.add(AnnotationLiterals.CDI_COMPONENT_PROVIDER);
            classLevelBeans.add(AnnotationLiterals.WELD_REQUEST_SCOPE);
        }

        configure(classLevelConfigMeta);

        if (!classLevelConfigMeta.useExisting) {
            // the container startup is delayed in case useExisting=true,
            // otherwise we want to start early, so parameterized test method sources that use CDI can work
            startContainer(classLevelBeans, classLevelExtensions, classLevelDisableDiscovery);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (resetPerTest) {
            Method testMethod = context.getRequiredTestMethod();
            AnnotationFinder finder = new AnnotationFinder(testMethod, TEST_ANNOTATIONS);

            ConfigMeta configMeta = new ConfigMeta(classLevelConfigMeta);
            finder.forEach(AddConfig.class, configMeta::config);
            finder.ifPresent(Configuration.class, configMeta::config);
            finder.ifPresent(AddConfigBlock.class, configMeta::config);
            configure(configMeta);

            List<AddExtension> extensions = new ArrayList<>(classLevelExtensions);
            List<AddBean> beans = new ArrayList<>(classLevelBeans);
            finder.forEach(AddExtension.class, extensions::add);
            finder.forEach(AddBean.class, beans::add);

            boolean disabledDiscovery = finder.stream(DisableDiscovery.class)
                    .findFirst()
                    .map(DisableDiscovery::value)
                    .orElse(classLevelDisableDiscovery);

            startContainer(beans, extensions, disabledDiscovery);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (resetPerTest) {
            releaseConfig();
            stopContainer();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        stopContainer();
        releaseConfig();
        callAfterStop();
    }

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
                                               ReflectiveInvocationContext<Constructor<T>> invocationContext,
                                               ExtensionContext extensionContext) throws Throwable {

        if (resetPerTest) {
            // Junit creates test instance
            return invocation.proceed();
        }

        // we need to start container before the test class is instantiated, to honor @BeforeAll that
        // creates a custom MP config
        if (container == null) {
            // at this early stage the class should be checked whether it is annotated with
            // @TestInstance(TestInstance.Lifecycle.PER_CLASS) to start correctly the container
            TestInstance testClassAnnotation = testClass.getAnnotation(TestInstance.class);
            if (testClassAnnotation != null && testClassAnnotation.value().equals(TestInstance.Lifecycle.PER_CLASS)) {
                throw new RuntimeException("When a class is annotated with @HelidonTest, "
                                           + "it is not compatible with @TestInstance(TestInstance.Lifecycle.PER_CLASS)"
                                           + "annotation, as it is a Singleton CDI Bean.");
            }
            startContainer(classLevelBeans, classLevelExtensions, classLevelDisableDiscovery);
        }

        // we need to replace instantiation with CDI lookup, to properly injection into fields (and constructors)
        invocation.skip();

        Class<T> declaringClass = invocationContext.getExecutable().getDeclaringClass();
        return container.select(declaringClass).get();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Executable executable = parameterContext.getParameter().getDeclaringExecutable();
        Class<?> paramType = parameterContext.getParameter().getType();

        if (resetPerTest) {
            if (executable instanceof Constructor) {
                throw new ParameterResolutionException(
                        "When a test class is annotated with @HelidonTest(resetPerMethod=true), constructor must not have "
                        + "parameters.");
            }
        } else {
            // we need to start container before the test class is instantiated, to honor @BeforeAll that
            // creates a custom MP config
            if (container == null) {
                startContainer(classLevelBeans, classLevelExtensions, classLevelDisableDiscovery);
            }
        }

        return switch (executable) {
            case Constructor<?> ignored -> !container.select(paramType).isUnsatisfied();
            case Method ignored -> paramType.equals(SeContainer.class) || paramType.equals(WebTarget.class);
        };
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Executable executable = parameterContext.getParameter().getDeclaringExecutable();
        Class<?> paramType = parameterContext.getParameter().getType();

        if (paramType.isPrimitive()) {
            // must return non-null for primitive
            return Array.get(Array.newInstance(paramType, 1), 0);
        }

        return switch (executable) {
            case Method ignored -> resolveMethodParameter(paramType);
            case Constructor<?> ignored -> null; // construction is done by CDI
        };
    }

    private Object resolveMethodParameter(Class<?> type) {
        if (type.equals(SeContainer.class)) {
            return container;
        }
        if (type.equals(WebTarget.class)) {
            return container.select(WebTarget.class).get();
        }
        return null;
    }

    private void configure(ConfigMeta configMeta) {
        if (config != null) {
            configProviderResolver.releaseConfig(config);
        }
        if (!configMeta.useExisting) {
            // only create a custom configuration if not provided by test method/class
            ConfigBuilder builder = configProviderResolver.getBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDiscoveredConverters();
            configMeta.configSources.forEach(builder::withSources);
            config = builder.build();
            configProviderResolver.registerConfig(config, Thread.currentThread().getContextClassLoader());
        }
    }

    private void releaseConfig() {
        if (configProviderResolver != null && config != null) {
            configProviderResolver.releaseConfig(config);
            config = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void startContainer(List<AddBean> beanAnnotations,
                                List<AddExtension> extensionAnnotations,
                                boolean disableDiscovery) {

        // now let's prepare the CDI bootstrapping
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();

        if (disableDiscovery) {
            initializer.disableDiscovery();
        }

        initializer.addExtensions(new AddBeansExtension(testClass, beanAnnotations));

        for (AddExtension addExtension : extensionAnnotations) {
            Class<? extends Extension> extensionClass = addExtension.value();
            if (Modifier.isPublic(extensionClass.getModifiers())) {
                initializer.addExtensions(addExtension.value());
            } else {
                throw new IllegalArgumentException("Extension classes must be public, "
                                                   + "but " + extensionClass.getName() + " is not");
            }
        }

        container = initializer.initialize();
    }

    private void stopContainer() {
        if (container != null) {
            container.close();
            container = null;
        }
    }

    private void callAfterStop() {
        visitMethods(testClass, m -> m.isAnnotationPresent(AfterStop.class), m -> {
            if (m.getParameterCount() != 0) {
                throw new IllegalStateException("Method " + m + " is annotated with @AfterStop, but it has parameters");
            }
            if (!isStatic(m)) {
                throw new IllegalStateException("Method " + m + " is annotated with @AfterStop, but it is not static");
            }
            invoke(m, Void.class);
        });
    }

    @SuppressWarnings("ALL")
    private void validatePerClass() {
        Method[] methods = testClass.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class)) {
                if (AnnotationFinder.hasAny(method, TEST_ANNOTATIONS)) {
                    throw new RuntimeException("When a class is annotated with @HelidonTest, "
                                               + "there is a single CDI container used to invoke all "
                                               + "test methods on the class. Method " + method
                                               + " has an annotation that modifies container behavior.");
                }
            }
        }

        methods = testClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class)) {
                if (AnnotationFinder.hasAny(method, TEST_ANNOTATIONS)) {
                    throw new RuntimeException("When a class is annotated with @HelidonTest, "
                                               + "there is a single CDI container used to invoke all "
                                               + "test methods on the class. Method " + method
                                               + " has an annotation that modifies container behavior.");
                }
            }
        }

        if (testClass.isAnnotationPresent(AddJaxRs.class)
            && !testClass.isAnnotationPresent(DisableDiscovery.class)) {
            throw new RuntimeException("@AddJaxRs annotation should be used only"
                                       + " with @DisableDiscovery annotation.");
        }
    }

    @SuppressWarnings("ALL")
    private void validatePerTest() {
        Constructor<?>[] constructors = testClass.getConstructors();
        if (constructors.length > 1) {
            throw new RuntimeException("When a class is annotated with @HelidonTest(resetPerTest=true),"
                                       + " the class must have only a single no-arg constructor");
        }
        if (constructors.length == 1) {
            Constructor<?> c = constructors[0];
            if (c.getParameterCount() > 0) {
                throw new RuntimeException("When a class is annotated with @HelidonTest(resetPerTest=true),"
                                           + " the class must have a no-arg constructor");
            }
        }

        Field[] fields = testClass.getFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                throw new RuntimeException("When a class is annotated with @HelidonTest(resetPerTest=true),"
                                           + " injection into fields or constructor is not supported, as each"
                                           + " test method uses a different CDI container. Field " + field
                                           + " is annotated with @Inject");
            }
        }

        fields = testClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Inject.class)) {
                throw new RuntimeException("When a class is annotated with @HelidonTest(resetPerTest=true),"
                                           + " injection into fields or constructor is not supported, as each"
                                           + " test method uses a different CDI container. Field " + field
                                           + " is annotated with @Inject");
            }
        }
    }

    private static final class ConfigMeta {

        private final Map<String, String> configMap = new HashMap<>();
        private final List<ConfigSource> configSources = new ArrayList<>();
        private boolean useExisting;

        ConfigMeta() {
            // higher ordinal then all the defaults, system props and environment variables
            configMap.putIfAbsent(ConfigSource.CONFIG_ORDINAL, "1000");
            configMap.put("mp.initializer.allow", "true");
            configMap.put("mp.initializer.no-warn", "true");
            configMap.put("server.port", "0");
            configMap.put("mp.config.profile", "test");
            configSources.add(MpConfigSources.create(AddConfig.class.getName(), configMap));
        }

        ConfigMeta(ConfigMeta configMeta) {
            for (ConfigSource configSource : configMeta.configSources) {
                if (!AddConfig.class.getName().equals(configSource.getName())) {
                    configSources.add(configSource);
                }
            }
            useExisting = configMeta.useExisting;
            configMap.putAll(configMeta.configMap);
            configSources.add(MpConfigSources.create(AddConfig.class.getName(), configMap));
        }

        void config(AddConfig config) {
            configMap.put(config.key(), config.value());
        }

        void config(Configuration config) {
            useExisting = config.useExisting();
            configMap.put("mp.config.profile", config.profile());
            for (String configSource : config.configSources()) {
                String name = configSource.trim();
                int idx = name.lastIndexOf('.');
                String type = idx > -1 ? name.substring(idx + 1) : "properties";
                try {
                    Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(name);
                    urls.asIterator().forEachRemaining(url -> this.configSources.add(MpConfigSources.create(type, url)));
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read \"" + name + "\" from classpath", e);
                }
            }
        }

        void config(AddConfigBlock config) {
            configSources.add(MpConfigSources.create(config.type(), new StringReader(config.value())));
        }

        void config(Method method) {
            ConfigSource configSource = invoke(method, ConfigSource.class);
            configSources.add(configSource);
        }
    }
}
