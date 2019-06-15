package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.reactive.Flow.Publisher;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * String entity reader.
 */
public final class StringEntityReader implements EntityReader<String> {

    @Override
    public boolean accept(Class<?> type, ContentInfo info) {
        return type.isAssignableFrom(String.class);
    }

    @Override
    public CompletionStage<? extends String> readEntity(
            Publisher<DataChunk> publisher, Class<? super String> type,
            ContentInfo info, Charset defaultCharset) {

        try {
            return ByteArrayEntityReader.read(publisher)
                    .thenApply(bytes -> new String(bytes,
                            info.charset(defaultCharset)));
        } catch (IllegalStateException ex) {
            CompletableFuture<String> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException(
                    "Cannot produce a string with the expected charset.", ex));
            return result;
        }
    }
}
