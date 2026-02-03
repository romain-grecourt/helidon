/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.common;

/**
 * Deprecation utility.
 */
public final class DeprecationSupport {

    private DeprecationSupport() {
    }

    /**
     * Require a method override.
     *
     * @param obj            object whose class should override the method
     * @param declaringClass class that declares the method to be overridden
     * @param methodName     the name of the method to be overridden
     * @param parameterTypes the parameter types of the method to be overridden
     * @throws UnsupportedOperationException if the method is not overridden
     * @throws IllegalStateException         if the method is not found using reflection
     */
    public static void requireOverride(Object obj, Class<?> declaringClass, String methodName, Class<?>... parameterTypes) {
        try {
            var classMethod = obj.getClass().getMethod(methodName, parameterTypes);
            if (classMethod.getDeclaringClass() == declaringClass) {
                throw new UnsupportedOperationException(
                        "%s does not override method %s(%s)".formatted(obj.getClass(), methodName, parameterTypes));
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
