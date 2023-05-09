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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webclient.http1.Http1ClientResponse;

import jakarta.ws.rs.ProcessingException;
import org.glassfish.jersey.client.ClientRequest;

import static org.glassfish.jersey.client.ClientProperties.OUTBOUND_CONTENT_LENGTH_BUFFER;

/**
 * A utility class that converts outbound client entity to a class understandable by Helidon.
 * Based on the {@link HelidonEntityType} an entity writer is provided to be registered by Helidon client
 * and an Entity is provided to be submitted by the Helidon Client.
 */
class HelidonEntity {

    private HelidonEntity() {
    }

    /**
     * HelidonEntity type chosen by HelidonEntityType.
     */
    enum HelidonEntityType {
        /**
         * Simplest structure. Loads all data to the memory.
         */
        BYTE_ARRAY_OUTPUT_STREAM,
        /**
         * Readable ByteChannel that is capable of sending data in chunks.
         * Capable of caching of bytes before the data are consumed by Helidon.
         */
        READABLE_BYTE_CHANNEL,
        /**
         * Helidon most native entity. Could be slower than {@link #READABLE_BYTE_CHANNEL}.
         */
        // Check LargeDataTest with OUTPUT_STREAM_MULTI
        OUTPUT_STREAM_MULTI
    }

    /**
     * Convert Jersey {@code OutputStream} to an entity based on the client request use case and submits to the provided
     * {@code Http1ClientRequest}.
     *
     * @param type            the type of the Helidon entity.
     * @param requestContext  Jersey {@link ClientRequest} providing the entity {@code OutputStream}.
     * @param request         Helidon {@code WebClientRequestBuilder} which is used to submit the entity
     * @param executorService {@link ExecutorService} that fills the entity instance for Helidon with data from Jersey
     *                        {@code OutputStream}.
     * @return Helidon Client response completion stage.
     */
    static Http1ClientResponse submit(HelidonEntityType type,
                                      ClientRequest requestContext,
                                      Http1ClientRequest request,
                                      ExecutorService executorService) {
        final int bufferSize = requestContext.resolveProperty(OUTBOUND_CONTENT_LENGTH_BUFFER, 8192);
        switch (type) {
            case BYTE_ARRAY_OUTPUT_STREAM -> {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
                requestContext.setStreamProvider(contentLength -> baos);
                ((ProcessingRunnable) requestContext::writeEntity).run();
                return request.submit(baos.toByteArray());
            }
            case READABLE_BYTE_CHANNEL -> {
                final OutputStreamChannel channel = new OutputStreamChannel(bufferSize);
                requestContext.setStreamProvider(contentLength -> channel);
                executorService.execute((ProcessingRunnable) requestContext::writeEntity);
                return request.submit(channel);
            }
            default -> throw new UnsupportedOperationException("Unsupported entity type: " + type);
        }
    }

    @FunctionalInterface
    private interface ProcessingRunnable extends Runnable {
        void runOrThrow() throws IOException;

        @Override
        default void run() {
            try {
                runOrThrow();
            } catch (IOException e) {
                throw new ProcessingException("Error writing entity:", e);
            }
        }
    }
}
