/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a static method to provide server features.
 * <p>
 * Method(s) annotated with this annotation will be invoked before methods annotated with {@link SetUpServer}.
 * <p>
 * E.g.
 * <pre>
 * &#064;SetUpFeatures
 * static List&lt;? extends ServerFeature&gt; features() {
 *     return List.of(StaticContentFeature.builder()
 *                 .welcome("index.html")
 *                 .addClasspath(cp -> cp.location("/WEB"))
 *                 .build());
 * }</pre>
 *
 * @see SetUpServer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SetUpFeatures {
    /**
     * Configure server features discovery.
     * <p>
     * If {@code true}, the server is configured first with the features discovered from the class-path,
     * and then with the features returned by the annotated method.
     * <p>
     * If {@code false}, the server is configured only with the features returned by the annotated method.
     *
     * @return whether to discover server features
     */
    boolean value() default true;
}
