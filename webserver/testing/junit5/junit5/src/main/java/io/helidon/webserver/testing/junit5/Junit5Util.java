/*
 * Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

import java.lang.reflect.Parameter;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.spi.HelidonJunitExtension;

/**
 * Utility methods for JUnit5 extensions.
 * @deprecated use {@link HelidonJunitExtension#socketName(java.lang.reflect.Parameter)} instead
 */
@Deprecated(forRemoval = true, since = "4.4.0")
public final class Junit5Util {
    private Junit5Util() {
    }

    private static final HelidonJunitExtension NOOP = new HelidonJunitExtension() {
    };

    /**
     * Discover socket name using {@link Socket} annotation.
     * If none found, {@link WebServer#DEFAULT_SOCKET_NAME} is returned.
     *
     * @param parameter parameter to check
     * @return name of the socket the parameter belongs to
     */
    public static String socketName(Parameter parameter) {
        return NOOP.socketName(parameter);
    }
}
