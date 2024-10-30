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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reflection helper.
 */
class ReflectionHelper {

    private ReflectionHelper() {
        // cannot be instanciated
    }

    /**
     * Invoke the given consumer for each method of the class that matches the given predicate.
     *
     * @param type      type
     * @param predicate predicate
     * @param consumer  consumer
     */
    static void visitMethods(Class<?> type, Predicate<Method> predicate, Consumer<Method> consumer) {
        visitType(type, t -> Arrays.stream(t.getDeclaredMethods())
                .filter(predicate)
                .forEach(consumer));
    }

    /**
     * Invoke the given consumer on each method that matches the given method in the class hierarchy.
     *
     * @param method   method
     * @param consumer consumer
     */
    static void visitMethod(Method method, Consumer<Method> consumer) {
        visitType(method.getDeclaringClass(), t -> Arrays.stream(t.getDeclaredMethods())
                .filter(m -> isOverride(method, m))
                .forEach(consumer));
    }

    /**
     * Invoke the given consumer on each type in the class hierarchy.
     *
     * @param type     type
     * @param consumer consumer
     */
    static void visitType(Class<?> type, Consumer<Class<?>> consumer) {
        Deque<Class<?>> stack = new ArrayDeque<>();
        stack.push(type);
        while (!stack.isEmpty()) {
            Class<?> e = stack.pop();
            if (e.getPackage().getName().startsWith("java.")) {
                continue;
            }
            consumer.accept(e);
            stack.add(e.getSuperclass());
            Arrays.stream(e.getInterfaces()).forEach(stack::push);
        }
    }

    /**
     * Invoke the given consumer on each annnotation of the annotation hierarchy.
     *
     * @param annotation annotation
     * @param predicate  traversing predicate
     * @param consumer   consumer
     */
    static void visitAnnotation(Annotation annotation,
                                Predicate<Class<? extends Annotation>> predicate,
                                Consumer<Annotation> consumer) {

        Deque<Annotation> stack = new ArrayDeque<>();
        stack.push(annotation);
        while (!stack.isEmpty()) {
            Annotation e = stack.pop();
            Class<? extends Annotation> type = e.annotationType();
            if (type.getPackage().getName().startsWith("java.")) {
                continue;
            }
            consumer.accept(e);
            if (!predicate.test(type)) {
                Arrays.stream(type.getAnnotations()).forEach(stack::push);
            }
        }
    }

    /**
     * Test method override.
     *
     * @param override overriding method
     * @param method   super class or interface method
     * @return {@code true} if overrides, {@code false} otherwise
     */
    static boolean isOverride(Method override, Method method) {
        return method.getName().equals(override.getName())
               && override.getReturnType().isAssignableFrom(method.getReturnType())
               && Arrays.equals(method.getParameterTypes(), override.getParameterTypes());
    }

    /**
     * Test if a method is static.
     *
     * @param method method
     * @return {@code true} if static, {@code false} otherwise
     */
    static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Test if a method is static and has the given annotation.
     *
     * @param method         method
     * @param annotationType annotation type
     * @return {@code true} if matches, {@code false} otherwise
     */
    static boolean isStatic(Method method, Class<? extends Annotation> annotationType) {
        return isStatic(method) && method.isAnnotationPresent(annotationType);
    }

    /**
     * Invoke a method.
     *
     * @param method method
     * @param type   return type
     * @param args   arguments
     * @param <T>    return type
     * @return invocation result
     */
    static <T> T invoke(Method method, Class<T> type, Object... args) {
        try {
            method.setAccessible(true);
            Object value = method.invoke(args);
            return type.cast(value);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
