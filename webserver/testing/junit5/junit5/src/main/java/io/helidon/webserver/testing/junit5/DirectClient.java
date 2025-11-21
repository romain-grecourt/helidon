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

import java.net.URI;

import io.helidon.http.Method;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientConfig;
import io.helidon.webclient.http1.Http1ClientRequest;
import io.helidon.webserver.Router;
import io.helidon.webserver.http.HttpRouting;

/**
 * In-memory {@link io.helidon.webclient.http1.Http1Client}.
 */
public class DirectClient extends DirectClientBase<DirectClient, Http1Client, Http1ClientRequest>
        implements Http1Client {

    private final Http1Client httpClient;

    /**
     * Create a direct client for HTTP routing.
     *
     * @param routing routing to use
     */
    public DirectClient(HttpRouting.Builder routing) {
        super(routing);
        httpClient = Http1Client.builder()
                .baseUri(URI.create("http://helidon-unit:65000"))
                .build();
    }

    @Override
    public Http1ClientConfig prototype() {
        return httpClient.prototype();
    }

    @Override
    protected Http1ClientRequest request(Method method, DirectSocket socket, Router router) {
        return httpClient.method(method).connection(new DirectClientConnection(socket, router));
    }
}
