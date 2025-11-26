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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.common.GenericType;
import io.helidon.common.UncheckedException;

/**
 * Reflection helper.
 */
class ReflectionHelper {

    private ReflectionHelper() {
        // cannot be instanciated
    }

    /**
     * Collect all the methods that have the given method signature in its type hierarchy.
     *
     * @param method method
     * @return hierarchy
     */
    static List<Method> methodHierarchy(Method method) {
        return typeHierarchy(method.getDeclaringClass()).stream()
                .flatMap(t -> Stream.of(t.getDeclaredMethods()))
                .filter(override -> isOverride(method, override))
                .toList();
    }

    /**
     * Test if the given method overrides another.
     *
     * @param method   base method
     * @param override override method
     * @return {@code true} if overrides, {@code false} otherwise
     */
    static boolean isOverride(Method method, Method override) {
        return override.getName().equals(method.getName())
               && override.getReturnType().isAssignableFrom(method.getReturnType())
               && Arrays.equals(override.getParameterTypes(), method.getParameterTypes());
    }

    /**
     * Collect all the methods in the type hierarchy of the given type.
     *
     * @param type type
     * @return methods
     */
    static List<Method> methods(Class<?> type) {
        return typeHierarchy(type).stream()
                .flatMap(t -> Stream.of(t.getDeclaredMethods()))
                .collect(ArrayList::new, (l, m) -> {
                    for (var e : l) {
                        if (isOverride(m, e)) {
                            return;
                        }
                    }
                    l.add(m);
                }, ArrayList::addAll);
    }

    /**
     * Collect all types in the type hiearchy of the given type.
     *
     * @param type type
     * @return types
     */
    static List<Class<?>> typeHierarchy(Class<?> type) {
        List<Class<?>> result = new ArrayList<>();
        Deque<Class<?>> stack = new ArrayDeque<>();
        stack.push(type);
        while (!stack.isEmpty()) {
            Class<?> e = stack.pop();
            if (e.getPackage().getName().startsWith("java.")) {
                continue;
            }
            result.add(e);
            if (e.getSuperclass() != null) {
                stack.push(e.getSuperclass());
            }
            List.of(e.getInterfaces()).forEach(stack::push);
        }
        return result;
    }

    /**
     * Collect all effective annotations of an annotation class.
     *
     * @param annotationType annotation type
     * @return annotations
     */
    static List<Annotation> annotationHierarchy(Class<? extends Annotation> annotationType) {
        List<Annotation> result = new ArrayList<>();
        Deque<Annotation> stack = new ArrayDeque<>();
        List.of(annotationType.getAnnotations()).forEach(stack::push);
        while (!stack.isEmpty()) {
            Annotation e = stack.pop();
            Class<? extends Annotation> type = e.annotationType();
            if (!type.getPackage().getName().startsWith("io.helidon.")) {
                continue;
            }
            result.add(e);
            List.of(type.getAnnotations()).forEach(stack::push);
        }
        return result;
    }

    /**
     * Annotated element.
     *
     * @param element     element
     * @param annotations annotations
     */
    record Annotated<T extends Annotation>(AnnotatedElement element, List<T> annotations) {

        private static Annotated<?> create(AnnotatedElement element) {
            return new Annotated<>(element, Stream.of(element.getAnnotations())
                    .flatMap(a -> Stream.concat(Stream.of(a), annotationHierarchy(a.annotationType()).stream()))
                    .toList());
        }
    }

    /**
     * Get all annotations for a method and its hierarchy.
     *
     * @param method method
     * @return annotations
     */
    static List<Annotated<?>> annotated(Method method) {
        return annotated(methodHierarchy(method));
    }

    /**
     * Get all annotations for a class and its hierarchy.
     *
     * @param type type
     * @return annotations
     */
    static List<Annotated<?>> annotated(Class<?> type) {
        return annotated(typeHierarchy(type));
    }

    /**
     * Get all annotations for the given elements.
     *
     * @param elements elements
     * @return annotations
     */
    static List<Annotated<?>> annotated(AnnotatedElement... elements) {
        return annotated(List.of(elements));
    }


    /**
     * Get all annotations for the given elements.
     *
     * @param elements elements
     * @return annotations
     */
    static List<Annotated<?>> annotated(List<? extends AnnotatedElement> elements) {
        return elements.stream().map(Annotated::create)
                .filter(a -> !a.annotations().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Filter annotations of a given type.
     *
     * @param annotated annotations
     * @param aType     annotation type
     * @param <T>       container type
     * @return annotations
     */
    static <T extends Annotation> List<Annotated<T>> filterAnnotated(List<Annotated<?>> annotated, Class<T> aType) {
        Predicate<Annotation> predicate = a -> a.annotationType().equals(aType);
        return annotated.stream()
                .filter(a -> a.annotations.stream().anyMatch(predicate))
                .map(it -> new Annotated<>(it.element, it.annotations.stream()
                        .filter(predicate)
                        .map(aType::cast)
                        .toList()))
                .toList();
    }

    /**
     * Filter annotations of a given type.
     *
     * @param annotations    annotations
     * @param annotationType annotation type
     * @param <T>            container type
     * @return annotations
     */
    static <T extends Annotation> Stream<T> filterAnnotations(List<Annotated<?>> annotations, Class<T> annotationType) {
        return filterAnnotated(annotations, annotationType).stream()
                .flatMap(it -> it.annotations.stream());
    }

    /**
     * Checks that the given method is static.
     *
     * @param method method
     * @return Method
     * @throws java.lang.IllegalArgumentException if the given class is not public
     */
    static Method requireStatic(Method method) throws IllegalArgumentException {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(method + " is not static");
        }
        return method;
    }

    /**
     * Invoke a method.
     *
     * @param type     return type, may be {@code null}
     * @param method   method
     * @param instance instance, may be {@code null} for static methods
     * @param args     arguments
     * @param <T>      return type
     * @return invocation result, {@code null} if {@code type} is {@code null}
     */
    static <T> T invokeMethod(GenericType<T> type, Method method, Object instance, Object... args) {
        try {
            method.setAccessible(true);
            Object value = method.invoke(instance, args);
            return type.cast(value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Invoke a method.
     *
     * @param method   method
     * @param instance instance, may be {@code null} for static methods
     * @param args     arguments
     */
    static void invokeMethod(Method method, Object instance, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new UncheckedException(e);
        }
    }
}
