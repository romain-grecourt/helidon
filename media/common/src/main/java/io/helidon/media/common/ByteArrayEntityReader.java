package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.Utils;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Entity reader for {@code byte[]}.
 */
public final class ByteArrayEntityReader implements EntityReader<byte[]> {

    @Override
    public boolean accept(Class<?> type, ContentInfo info) {
        return type.isAssignableFrom(byte[].class);
    }

    @Override
    public CompletionStage<byte[]> readEntity(
            Publisher<DataChunk> publisher,
            Class<? super byte[]> type, ContentInfo info,
            Charset defaultCharset) {

        return read(publisher);
    }

    public static CompletionStage<byte[]> read(Publisher<DataChunk> publisher) {
        ContentSubscriber subscriber = new ContentSubscriber();
        publisher.subscribe(subscriber);
        return subscriber.future;
    }

    private static final class ContentSubscriber
            implements Subscriber<DataChunk> {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> future =
                new CompletableFuture<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(DataChunk item) {
            try {
                Utils.write(item.data(), bytes);
            } catch (IOException e) {
                onError(new IllegalArgumentException(
                        "Cannot convert byte buffer to a byte array!", e));
            } finally {
                item.release();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            future.complete(bytes.toByteArray());
        }
    }
}
