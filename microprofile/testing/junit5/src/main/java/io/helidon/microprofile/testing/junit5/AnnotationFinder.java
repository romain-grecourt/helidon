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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.helidon.microprofile.testing.junit5.ReflectionHelper.visitAnnotation;
import static io.helidon.microprofile.testing.junit5.ReflectionHelper.visitMethod;
import static io.helidon.microprofile.testing.junit5.ReflectionHelper.visitType;

/**
 * Annotation finder.
 */
class AnnotationFinder {

    private final List<Annotation> annotations = new ArrayList<>();

    /**
     * Create an annotation finder for a type.
     *
     * @param type            type to visit
     * @param annotationTypes meta annotation types
     */
    AnnotationFinder(Class<?> type, Set<Class<? extends Annotation>> annotationTypes) {
        visitType(type, t -> Arrays.stream(t.getAnnotations())
                .forEach(annotation -> visitAnnotation(annotation, annotationTypes::contains, annotations::add)));
    }

    /**
     * Create an annotation finder for a method.
     *
     * @param method          method to visit
     * @param annotationTypes meta annotation types
     */
    AnnotationFinder(Method method, Set<Class<? extends Annotation>> annotationTypes) {
        visitMethod(method, m -> Arrays.stream(m.getAnnotations())
                .forEach(annotation -> visitAnnotation(annotation, annotationTypes::contains, annotations::add)));
    }

    /**
     * Test if the given element is annotated with any of the given annotation types.
     *
     * @param element         element
     * @param annotationTypes annotation types
     * @return {@code true} if matching, {@code false} otherwise
     */
    static boolean hasAny(AnnotatedElement element, Collection<Class<? extends Annotation>> annotationTypes) {
        return annotationTypes.stream().anyMatch(element::isAnnotationPresent);
    }

    /**
     * Find annotations by type.
     *
     * @param annotationType annotation type
     * @param <T>            annotation type
     * @return annotations stream
     */
    <T extends Annotation> Stream<T> stream(Class<T> annotationType) {
        return annotations.stream()
                .filter(a -> a.annotationType().equals(annotationType))
                .map(annotationType::cast);
    }

    /**
     * Invoke the given consumer for each annotation of the given type.
     *
     * @param annotationType annotation type
     * @param consumer       consumer
     * @param <T>            annotation type
     */
    <T extends Annotation> void forEach(Class<T> annotationType, Consumer<T> consumer) {
        stream(annotationType).forEach(consumer);
    }

    /**
     * Invoke the given consumer for each annotation contained by the given type.
     *
     * @param containerType container type
     * @param mapper        mapper function
     * @param consumer      consumer
     * @param <T>           container type
     * @param <U>           annotation type
     */
    <T extends Annotation, U extends Annotation> void forEach(Class<T> containerType,
                                                              Function<T, U[]> mapper,
                                                              Consumer<U> consumer) {
        stream(containerType)
                .map(mapper)
                .flatMap(Arrays::stream)
                .forEach(consumer);
    }

    /**
     * Invoke the given consumer on the first annotation of the given type, if present.
     *
     * @param annotationType annotation type
     * @param consumer       consumer
     * @param <T>            annotation type
     */
    <T extends Annotation> void ifPresent(Class<T> annotationType, Consumer<T> consumer) {
        stream(annotationType).findFirst().ifPresent(consumer);
    }

    /**
     * Test if there is at least one annotation of the given type.
     *
     * @param annotationType annotation type
     * @param <T>            annotation type
     * @return {@code true} if found, {@code false} otherwise
     */
    <T extends Annotation> boolean isPresent(Class<T> annotationType) {
        return stream(annotationType).findFirst().isPresent();
    }
}
