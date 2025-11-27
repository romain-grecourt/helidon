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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.Weight;
import io.helidon.config.spi.ConfigNode;
import io.helidon.config.spi.ConfigSource;
import io.helidon.config.spi.LazyConfigSource;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;

/**
 * A lazy config source that exposes {@code test.server.port} if the server is initialized.
 */
@Service.Singleton
@Weight(10000D)
final class WebServerConfigSource implements ConfigSource, LazyConfigSource, WebServerRef {

    private final AtomicReference<WebServer> ref = new AtomicReference<>();

    @Override
    public Optional<ConfigNode> node(String key) {
        if ("test.server.port".equals(key)) {
            return Optional.ofNullable(ref.get())
                    .map(s -> ConfigNode.ValueNode.create(String.valueOf(s.port())));
        }
        return Optional.empty();
    }

    @Override
    public void server(WebServer server) {
        ref.set(server);
    }
}
