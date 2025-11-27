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
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;

/**
 * A {@link WebServer} factory backed by a mutable reference.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 20)
final class WebServerProvider implements Supplier<WebServer>, WebServerRef {

    private final AtomicReference<WebServer> ref = new AtomicReference<>();

    @Override
    public WebServer get() {
        var server = ref.get();
        if (server == null) {
            throw new NoSuchElementException("Server is not initialized");
        }
        return server;
    }

    @Override
    public void server(WebServer server) {
        ref.set(server);
    }
}
