/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.media.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;

/**
 * A {@link Flow.Subscriber subscriber} that can subscribe to a {@link Flow.Publisher publisher} of {@link DataChunk data chunk}
 * and make the data available for consumption via standard blocking {@link InputStream} API.
 *
 * This {@link InputStream} is not thread-safe, concurrent accesses should not be allowed and invocations of read() should be
 * synchronized by the consumer for any state updates to be visible cross-threads.
 *
 * The following assumptions are made about the publisher:
 * <ul>
 * <li>{@code request} is invoked only after one chunk has been consumed</li>
 * <li>The number of chunks requested is always 1</li>
 * <li>The source {@link Flow.Publisher} fully conforms to the reactive-streams specification with respect to:
 * <ul>
 * <li>Total order of {@code onNext}, {@code onComplete}, {@code onError} calls</li>
 * <li>Follows back pressure: {@code onNext} is not called until more chunks are requested</li>
 * <li>Relaxed ordering of calls to {@code request}, allows to request even after onComplete/onError</li>
 * </ul>
 * </li>
 * </ul>
 */
public class PublisherInputStream extends InputStream implements Flow.Publisher<DataChunk> {

    private static final Logger LOGGER = Logger.getLogger(PublisherInputStream.class.getName());

    private final Flow.Publisher<DataChunk> originalPublisher;
    private CompletableFuture<DataChunk> current = new CompletableFuture<>();
    private CompletableFuture<DataChunk> next = current;
    private volatile Flow.Subscription subscription;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private byte[] oneByte;

    /**
     * Wraps the supplied publisher and adds a blocking {@link InputStream} based nature.
     * It is illegal to subscribe to the returned publisher.
     *
     * @param originalPublisher the original publisher to wrap
     */
    public PublisherInputStream(Flow.Publisher<DataChunk> originalPublisher) {
        this.originalPublisher = originalPublisher;
    }

    @Override
    public void close() {
        // assert: if current != next, next cannot ever be resolved with a chunk that needs releasing
        current.whenComplete(PublisherInputStream::releaseChunk);
        current = null; // any future read() will fail
    }

    @Override
    public int read() throws IOException {
        if (oneByte == null) {
            oneByte = new byte[1];
        }
        // Chunks are always non-empty, so r is either 1 (at least one byte is produced) or
        // negative (EOF)
        int r = read(oneByte, 0, 1);
        if (r < 0) {
            return r;
        }

        return oneByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (subscribed.compareAndSet(false, true)) {
            // subscribe just once
            subscribe();
        }
        if (current == null) {
            throw new IOException("Already closed");
        }
        try {
            DataChunk chunk = current.get(); // block until a processing data are available
            if (chunk == null) {
                return -1;
            }

            ByteBuffer currentBuffer = chunk.data();

            if (currentBuffer.position() == 0) {
                LOGGER.finest(() -> "Reading chunk ID: " + chunk.id());
            }

            int rem = currentBuffer.remaining();
            // read as much as possible
            if (len > rem) {
                len = rem;
            }
            currentBuffer.get(buf, off, len);

            // chunk is consumed entirely, release the chunk and prefetch a new chunk
            if (len == rem) {
                releaseChunk(chunk, null);
                current = next;
                subscription.request(1);
            }

            return len;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
        subscriber.onError(new UnsupportedOperationException("Subscribing on this publisher is not allowed!"));
    }

    private void subscribe() {
        originalPublisher.subscribe(new Flow.Subscriber<DataChunk>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                PublisherInputStream.this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(DataChunk item) {
                LOGGER.finest(() -> "Processing chunk: " + item.id());
                // set next to the next future before completing it
                // since completing next will unblock read() which which may set current to next
                // if all the data in current has been consumed
                CompletableFuture<DataChunk> prev = next;
                next = new CompletableFuture<>();
                // unblock read()
                prev.complete(item);
            }

            @Override
            public void onError(Throwable throwable) {
                // unblock read() with an ExecutionException wrapping the throwable
                // read() uses a try/catch and wraps the ExecutionException cause in an IOException
                next.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                // read() returns EOF if the chunk is null
                next.complete(null);
            }
        });
    }

    private static void releaseChunk(DataChunk chunk, Throwable th) {
        if (chunk != null && !chunk.isReleased()) {
            LOGGER.finest(() -> "Releasing chunk: " + chunk.id());
            chunk.release();
        }
    }
}
