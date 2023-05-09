/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.jersey.connector;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import io.helidon.common.Version;
import io.helidon.common.http.Http;
import io.helidon.common.socket.SocketOptions;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1Client.Http1ClientBuilder;

import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientAsyncExecutorLiteral;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.spi.ExecutorServiceProvider;

/**
 * A {@link Connector} that utilizes the Helidon HTTP Client to send and receive
 * HTTP request and responses.
 */
class HelidonConnector implements Connector {

    private static final String HELIDON_VERSION = "Helidon/" + Version.VERSION + " (java "
            + PropertiesHelper.getSystemProperty("java.runtime.version") + ")";
    static final Logger LOGGER = Logger.getLogger(HelidonConnector.class.getName());

    private final Http1Client webClient;

    private final ExecutorServiceKeeper executorServiceKeeper;
    private final HelidonEntity.HelidonEntityType entityType;

    private static final InputStream NO_CONTENT_INPUT_STREAM = new InputStream() {
        @Override
        public int read() {
            return -1;
        }
    };

    // internal implementation entity type, can be removed in the future
    // settable for testing purposes
    // see LargeDataTest

    static final String INTERNAL_ENTITY_TYPE = "jersey.connector.helidon.entity.type";

    HelidonConnector(final Client client, final Configuration config) {
        executorServiceKeeper = new ExecutorServiceKeeper(client);
        entityType = getEntityType(config);

        final Http1ClientBuilder webClientBuilder = Http1Client.builder();
//        HelidonStructures.createProxy(config).ifPresent(webClientBuilder::proxy);
        HelidonStructures.helidonConfig(config).ifPresent(webClientBuilder::config);

        int connectTimeout = ClientProperties.getValue(config.getProperties(), ClientProperties.CONNECT_TIMEOUT, 1000);
        webClientBuilder.channelOptions(SocketOptions.builder()
                                                     .connectTimeout(Duration.ofMillis(connectTimeout))
                                                     .build());

        HelidonStructures.createSSL(client.getSslContext()).ifPresent(webClientBuilder::tls);

        webClient = webClientBuilder.build();
    }

    @Override
    public ClientResponse apply(ClientRequest request) {
        return applyInternal(request);
    }

    @Override
    public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
        try {
            callback.response(applyInternal(request));
            return CompletableFuture.completedFuture(null);
        } catch (Throwable th) {
            callback.failure(th);
            return CompletableFuture.failedFuture(th);
        }
    }

    @Override
    public String getName() {
        return HELIDON_VERSION;
    }

    @Override
    public void close() {

    }

    private ClientResponse applyInternal(ClientRequest request) {
        final Http1ClientRequest webClientRequest = webClient.method(Http.Method.create(request.getMethod()));
        webClientRequest.uri(request.getUri());

        webClientRequest.headers(headers -> {
            headers.addAll(HelidonStructures.createHeaders(request.getRequestHeaders()));
            return headers;
        });

        for (String propertyName : request.getConfiguration().getPropertyNames()) {
            Object property = request.getConfiguration().getProperty(propertyName);
            if (!propertyName.startsWith("jersey") && property instanceof String) {
                webClientRequest.property(propertyName, (String) property);
            }
        }

        for (String propertyName : request.getPropertyNames()) {
            Object property = request.resolveProperty(propertyName, Object.class);
            if (!propertyName.startsWith("jersey") && property instanceof String) {
                webClientRequest.property(propertyName, (String) property);
            }
        }

        // TODO
        // HelidonStructures.createProxy(request).ifPresent(webClientRequestBuilder::proxy);

//        webClientRequestBuilder.followRedirects(request.resolveProperty(ClientProperties.FOLLOW_REDIRECTS, true));
//        webClientRequestBuilder.readTimeout(request.resolveProperty(ClientProperties.READ_TIMEOUT, 10000), TimeUnit.MILLISECONDS);

        Http1ClientResponse response;
        if (request.hasEntity()) {
            response = HelidonEntity.submit(
                    entityType, request, webClientRequest, executorServiceKeeper.getExecutorService(request));
        } else {
            response = webClientRequest.request();
        }

        return convertResponse(request, webClientRequest.resolvedUri(), response);
    }

    private ClientResponse convertResponse(ClientRequest requestContext,
                                           URI resolvedUri,
                                           Http1ClientResponse response) {

        final ClientResponse responseContext = new ClientResponse(new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return response.status().code();
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.familyOf(getStatusCode());
            }

            @Override
            public String getReasonPhrase() {
                return response.status().reasonPhrase();
            }
        }, requestContext);

        response.headers().forEach(it -> {
            for (String value : it.allValues()) {
                responseContext.getHeaders().add(it.name(), value);
            }
        });

        responseContext.setResolvedRequestUri(resolvedUri);

        final InputStream is = HelidonStructures.hasEntity(response)
                ? response.entity().as(InputStream.class)
                : NO_CONTENT_INPUT_STREAM;

        responseContext.setEntityStream(new FilterInputStream(is) {
            private final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() throws IOException {
                // Avoid idempotent close in the underlying input stream
                if (!closed.compareAndSet(false, true)) {
                    super.close();
                }
            }
        });
        return responseContext;
    }

    private static HelidonEntity.HelidonEntityType getEntityType(final Configuration config) {
        final String helidonType = ClientProperties.getValue(config.getProperties(),
                INTERNAL_ENTITY_TYPE, HelidonEntity.HelidonEntityType.READABLE_BYTE_CHANNEL.name());
        return HelidonEntity.HelidonEntityType.valueOf(helidonType);
    }

    private static class ExecutorServiceKeeper {
        private ExecutorService executorService;

        private ExecutorServiceKeeper(Client client) {
            final ClientConfig config = ((JerseyClient) client).getConfiguration();
            executorService = config.getExecutorService();
        }

        private ExecutorService getExecutorService(ClientRequest request) {
            if (executorService == null) {
                // cache for multiple requests
                this.executorService = request.getInjectionManager()
                                              .getInstance(ExecutorServiceProvider.class, ClientAsyncExecutorLiteral.INSTANCE)
                                              .getExecutorService();
            }
            return executorService;
        }
    }
}
