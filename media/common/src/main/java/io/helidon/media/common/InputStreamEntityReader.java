package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Entity reader for {@code InputStream}.
 */
public class InputStreamEntityReader implements EntityReader<InputStream> {

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return type.isAssignableFrom(InputStream.class);
    }

    @Override
    public CompletionStage<InputStream> readEntity(
            Publisher<DataChunk> publisher, Class<? super InputStream> type,
            InBoundScope scope) {

        return CompletableFuture.completedFuture(
                new PublisherInputStream(publisher));
    }
}
