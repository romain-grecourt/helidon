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

import java.util.concurrent.ExecutorService;

import io.helidon.http.Method;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.api.WebClientConfig;
import io.helidon.webclient.api.WebClientCookieManager;
import io.helidon.webclient.spi.Protocol;
import io.helidon.webclient.spi.ProtocolConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.http.HttpRouting;

/**
 * In-memory {@link io.helidon.webclient.http1.Http1Client}.
 */
public class DirectWebClient extends DirectClientBase<DirectWebClient, WebClient, HttpClientRequest> implements WebClient {

    private final WebClient webClient;

    /**
     * Create a direct client for HTTP routing.
     *
     * @param routing routing to use
     */
    public DirectWebClient(HttpRouting.Builder routing) {
        super(routing);
        webClient = WebClient.builder()
                .baseUri("http://helidon-unit:65000")
                .build();
    }

    @Override
    protected HttpClientRequest request(Method method, DirectSocket socket, Router router) {
        return webClient.method(method).connection(new DirectClientConnection(socket, router));
    }

    @Override
    public WebClientConfig prototype() {
        return webClient.prototype();
    }

    @Override
    public ExecutorService executor() {
        return webClient.executor();
    }

    @Override
    public WebClientCookieManager cookieManager() {
        return webClient.cookieManager();
    }

    @Override
    public <T, C extends ProtocolConfig> T client(Protocol<T, C> protocol, C protocolConfig) {
        throw new UnsupportedOperationException("Clients based on protocols cannot be used with DirectWebClient");
    }

    @Override
    public <T, C extends ProtocolConfig> T client(Protocol<T, C> protocol) {
        throw new UnsupportedOperationException("Clients based on protocols cannot be used with DirectWebClient");
    }
}
