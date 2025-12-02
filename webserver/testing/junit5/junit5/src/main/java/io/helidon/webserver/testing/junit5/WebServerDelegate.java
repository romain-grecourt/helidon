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

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.context.Context;
import io.helidon.common.tls.Tls;
import io.helidon.service.registry.Service;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

/**
 * A {@link WebServer} delegate backed by a mutable reference.
 */
@Service.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 20)
final class WebServerDelegate implements WebServer, WebServerRef {

    private final AtomicReference<WebServer> ref = new AtomicReference<>();

    private WebServer get() {
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

    @Override
    public WebServerConfig prototype() {
        return get().prototype();
    }

    @Override
    public WebServer start() {
        return get().start();
    }

    @Override
    public WebServer stop() {
        return get().stop();
    }

    @Override
    public boolean isRunning() {
        return get().isRunning();
    }

    @Override
    public int port() {
        return get().port();
    }

    @Override
    public boolean hasTls() {
        return get().hasTls();
    }

    @Override
    public int port(String socketName) {
        return get().port(socketName);
    }

    @Override
    public boolean hasTls(String socketName) {
        return get().hasTls(socketName);
    }

    @Override
    public Context context() {
        return get().context();
    }

    @Override
    public void reloadTls(Tls tls) {
        get().reloadTls(tls);
    }

    @Override
    public void reloadTls(String socketName, Tls tls) {
        get().reloadTls(socketName, tls);
    }
}
