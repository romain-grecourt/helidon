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

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Optional;

import io.helidon.http.Method;
import io.helidon.webclient.api.ClientRequest;
import io.helidon.webclient.api.HttpClient;
import io.helidon.webserver.Router;
import io.helidon.webserver.http.HttpRouting;

/**
 * In-memory base {@link io.helidon.webclient.api.HttpClient}.
 *
 * @param <SELF>    subclass type
 * @param <CLIENT>  client type
 * @param <REQUEST> client request type
 */
abstract class DirectClientBase<
        SELF extends DirectClientBase<SELF, CLIENT, REQUEST>,
        CLIENT extends HttpClient<REQUEST>,
        REQUEST extends ClientRequest<REQUEST>> implements HttpClient<REQUEST> {

    private String clientHost;
    private int clientPort;
    private Principal clientTlsPrincipal;
    private Certificate[] clientTlsCertificates;
    private String serverHost;
    private int serverPort;
    private Principal serverTlsPrincipal;
    private Certificate[] serverTlsCertificates;
    private boolean isTls;
    private final Router router;

    /**
     * Create a new instance.
     * @param routing routing
     */
    protected DirectClientBase(HttpRouting.Builder routing) {
        this.router = Router.builder().addRouting(routing).build();
    }

    /**
     * Create a specialized request.
     *
     * @param method method
     * @param socket socket
     * @param router router
     * @return specialized request
     */
    protected abstract REQUEST request(Method method, DirectSocket socket, Router router);

    @Override
    public REQUEST method(Method method) {
        if (clientHost == null) {
            clientHost = "localhost";
        }
        if (clientPort == 0) {
            clientPort = 64000;
        }
        if (serverPort == 0) {
            serverPort = 8080;
        }
        if (serverHost == null) {
            serverHost = "localhost";
        }
        var clientPeer = new DirectPeerInfo(
                InetSocketAddress.createUnresolved(clientHost, clientPort),
                clientHost,
                clientPort,
                Optional.ofNullable(clientTlsPrincipal),
                Optional.ofNullable(clientTlsCertificates));

        var localPeer = new DirectPeerInfo(
                InetSocketAddress.createUnresolved(serverHost, serverPort),
                serverHost,
                serverPort,
                Optional.ofNullable(serverTlsPrincipal),
                Optional.ofNullable(serverTlsCertificates));

        var socket = DirectSocket.create(localPeer, clientPeer, isTls);
        return request(method, socket, router);
    }

    /**
     * Whether to use tls (mark this connection as secure).
     *
     * @param tls use tls
     * @return updated client
     */
    public SELF setTls(boolean tls) {
        isTls = tls;
        return self();
    }

    /**
     * Client host.
     *
     * @param clientHost client host to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF clientHost(String clientHost) {
        this.clientHost = clientHost;
        return self();
    }

    /**
     * Client port.
     *
     * @param clientPort client port to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF clientPort(int clientPort) {
        this.clientPort = clientPort;
        return self();
    }

    /**
     * Client peer TLS principal.
     *
     * @param clientTlsPrincipal principal to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF clientTlsPrincipal(Principal clientTlsPrincipal) {
        this.clientTlsPrincipal = clientTlsPrincipal;
        return self();
    }

    /**
     * Client peer TLS certificates.
     *
     * @param clientTlsCertificates certificates to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF clientTlsCertificates(Certificate[] clientTlsCertificates) {
        this.clientTlsCertificates = clientTlsCertificates;
        return self();
    }

    /**
     * Server host.
     *
     * @param serverHost server host to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF serverHost(String serverHost) {
        this.serverHost = serverHost;
        return self();
    }

    /**
     * Server port.
     *
     * @param serverPort server port to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF serverPort(int serverPort) {
        this.serverPort = serverPort;
        return self();
    }

    /**
     * Server TLS principal.
     *
     * @param serverTlsPrincipal principal to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF serverTlsPrincipal(Principal serverTlsPrincipal) {
        this.serverTlsPrincipal = serverTlsPrincipal;
        return self();
    }

    /**
     * Server TLS certificates.
     *
     * @param serverTlsCertificates certificates to use in {@link io.helidon.common.socket.PeerInfo}
     * @return updated client
     */
    public SELF serverTlsCertificates(Certificate[] serverTlsCertificates) {
        this.serverTlsCertificates = serverTlsCertificates;
        return self();
    }

    /**
     * Call this method once testing is done, to carry out after stop operations on routers.
     */
    public void close() {
        this.router.afterStop();
    }

    @Override
    public void closeResource() {
        // Nothing to close in connection-less client
    }

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }
}
