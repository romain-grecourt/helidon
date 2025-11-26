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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;

@Service.Singleton
class WebServerRef {

    private final AtomicReference<WebServer> ref = new AtomicReference<>();

    WebServer get() {
        var server = ref.get();
        if (server != null) {
            return server;
        }
        throw new NoSuchElementException("Server reference not set");
    }

    void set(WebServer server) {
        this.ref.set(server);
    }
}
