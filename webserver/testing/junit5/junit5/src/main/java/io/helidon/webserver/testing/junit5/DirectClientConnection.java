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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.buffers.BufferData;
import io.helidon.common.buffers.DataReader;
import io.helidon.common.buffers.DataWriter;
import io.helidon.common.concurrency.limits.FixedLimit;
import io.helidon.common.socket.HelidonSocket;
import io.helidon.webclient.api.ClientConnection;
import io.helidon.webserver.ProtocolConfigs;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http1.Http1Config;
import io.helidon.webserver.http1.Http1ConnectionProvider;

class DirectClientConnection implements ClientConnection {

    private static final Logger LOGGER = System.getLogger(DirectClientConnection.class.getName());

    private final AtomicBoolean serverStarted = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final DataReader clientReader;
    private final DataWriter clientWriter;
    private final DirectClientServerContext serverContext;
    private final HelidonSocket socket;

    DirectClientConnection(HelidonSocket socket, Router router) {
        var serverToClient = new ArrayBlockingQueue<byte[]>(1024);
        var clientToServer = new ArrayBlockingQueue<byte[]>(1024);
        this.clientReader = reader(serverToClient);
        this.clientWriter = writer(clientToServer);
        this.socket = socket;
        var reader = reader(clientToServer);
        var writer = writer(serverToClient);
        this.serverContext = new DirectClientServerContext(router, socket, reader, writer);
    }

    @Override
    public HelidonSocket helidonSocket() {
        return socket;
    }

    @Override
    public DataReader reader() {
        return clientReader;
    }

    @Override
    public DataWriter writer() {
        return clientWriter;
    }

    @Override
    public void releaseResource() {
        closeResource();
    }

    @Override
    public void closeResource() {
        if (closed.compareAndSet(false, true)) {
            clientWriter.writeNow(BufferData.empty());
        }
    }

    @Override
    public String channelId() {
        return "unit-client";
    }

    @Override
    public void readTimeout(Duration readTimeout) {
        //NOOP
    }

    private DataWriter writer(ArrayBlockingQueue<byte[]> queue) {
        return new DataWriter() {
            @Override
            public void write(BufferData... buffers) {
                writeNow(buffers);
            }

            @Override
            public void write(BufferData buffer) {
                writeNow(buffer);
            }

            @Override
            public void writeNow(BufferData... buffers) {
                for (BufferData buffer : buffers) {
                    writeNow(buffer);
                }
            }

            @Override
            public void writeNow(BufferData buffer) {
                if (serverStarted.compareAndSet(false, true)) {
                    startServer();
                }
                var bytes = new byte[buffer.available()];
                buffer.read(bytes);
                try {
                    queue.put(bytes);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Thread interrupted", e);
                }
            }
        };
    }

    @SuppressWarnings("DuplicatedCode")
    private DataReader reader(ArrayBlockingQueue<byte[]> queue) {
        return new DataReader(() -> {
            byte[] data;
            try {
                data = queue.take();
            } catch (InterruptedException e) {
                throw new IllegalArgumentException("Thread interrupted", e);
            }
            if (data.length == 0) {
                return null;
            }
            return data;
        });
    }

    @SuppressWarnings({"deprecation", "resource"})
    private void startServer() {
        var conn = new Http1ConnectionProvider()
                .create(WebServer.DEFAULT_SOCKET_NAME, Http1Config.create(), ProtocolConfigs.create(List.of()))
                .connection(serverContext);

        serverContext.executor()
                .submit(() -> {
                    try {
                        conn.handle(FixedLimit.builder().permits(1024).build());
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Interrupted while starting server", e);
                    }
                });
    }
}
