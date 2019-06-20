package io.helidon.media.multipart.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * {@link InBoundMultiPart} entity reader.
 */
public class MultiPartEntityReader implements EntityReader<MultiPart> {

    public MultiPartEntityReader() {
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return MultiPart.class.isAssignableFrom(type);
    }

    @Override
    public CompletionStage<? extends MultiPart> readEntity(
            Publisher<DataChunk> chunks, Class<? super MultiPart> type,
            InBoundScope scope) {

        MultiPartDecoder decoder = new MultiPartDecoder(
                scope.contentType().parameters().get("boundary"),
                scope.defaultCharset(), scope.readers());
        chunks.subscribe(decoder);
        BufferingBodyPartSubscriber bodyPartSubscriber
                = new BufferingBodyPartSubscriber();
        decoder.subscribe(bodyPartSubscriber);
        CompletableFuture<MultiPart> future = new CompletableFuture<>();
        bodyPartSubscriber.getFuture().thenAccept(bodyParts -> {
            future.complete(new InBoundMultiPart(bodyParts));
        }).exceptionally((Throwable error) -> {
            future.completeExceptionally(error);
            return null;
        });
        return future;
    }
}
