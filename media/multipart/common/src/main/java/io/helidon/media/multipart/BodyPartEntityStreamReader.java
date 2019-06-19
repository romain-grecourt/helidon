package io.helidon.media.multipart;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityStreamReader;
import io.helidon.common.http.InBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link InBoundBodyPart} entity stream reader.
 */
public class BodyPartEntityStreamReader
        implements EntityStreamReader<InBoundBodyPart> {

    public BodyPartEntityStreamReader() {
    }

    @Override
    public boolean accept(Class<?> type, InBoundScope scope) {
        return BodyPart.class.isAssignableFrom(type);
    }

    @Override
    public Publisher<? extends InBoundBodyPart> readEntityStream(
            Publisher<DataChunk> chunks, Class<? super InBoundBodyPart> type,
            InBoundScope scope) {

        String boundary = scope.headers.first("boundary").orElse(null);
        if (boundary != null) {
            MultiPartDecoder decoder = new MultiPartDecoder(boundary,
                    scope.defaultCharset, scope.readers);
            chunks.subscribe(decoder);
            return decoder;
        } else {
            return new FailedPublisher<>(new IllegalStateException(
                    "boudary header is missing"));
        }
    }
}
