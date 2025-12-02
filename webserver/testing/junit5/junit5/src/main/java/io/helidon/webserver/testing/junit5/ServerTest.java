/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.testing.junit5.TestJunitExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.common.testing.virtualthreads.PinningRecorder.DEFAULT_THRESHOLD;

/**
 * In-process WebServer test.
 * <p>
 * A separate WebServer instance is started for each test class using an actual socket.
 * <p>
 * The server can be configured with static methods annotated:
 * <ul>
 *     <li>{@link SetUpRoute} to update {@link io.helidon.webserver.http.HttpRouting} and
 *     {@link io.helidon.webserver.ListenerConfig.Builder}</li>
 *     <li>{@link SetUpFeatures} to provide {@link io.helidon.webserver.spi.ServerFeature}</li>
 *     <li>{@link SetUpServer} to update the server builder {@link io.helidon.webserver.WebServerConfig}</li>
 * </ul>
 * <p>
 * Static methods annotated with {@link org.junit.jupiter.api.BeforeAll} can be used to set up global services.
 * <p>
 * E.g. Set up config
 * <pre>
 * &#064;BeforeAll
 * static void setUp() {
 *     Services.set(Config.class,
 *         Config.just(ConfigSources.create(Map.of("foo", "bar"))));
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(TestJunitExtension.class)
@ExtendWith(WebServerTestServerExtension.class)
@Inherited
public @interface ServerTest {
    /**
     * Time threshold for carrier thread blocking to be considered as pinning.
     *
     * @return threshold in milliseconds, {@code 20} is default
     */
    long pinningThreshold() default DEFAULT_THRESHOLD;

    /**
     * Whether to turn on pinning detection during test.
     *
     * @return true for turning detection on, {@code false} is default
     */
    boolean pinningDetection() default false;
}
