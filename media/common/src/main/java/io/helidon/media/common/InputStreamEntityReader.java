package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityReader;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Entity reader for {@code InputStream}.
 */
public class InputStreamEntityReader implements EntityReader<InputStream> {

    @Override
    public boolean accept(Class<?> type, ContentInfo info) {
        return type.isAssignableFrom(InputStream.class);
    }

    @Override
    public CompletionStage<InputStream> readEntity(
            Publisher<DataChunk> publisher, Class<? super InputStream> type,
            ContentInfo info, Charset defaultCharset) {

        return CompletableFuture.completedFuture(
                new PublisherInputStream(publisher));
    }
}
