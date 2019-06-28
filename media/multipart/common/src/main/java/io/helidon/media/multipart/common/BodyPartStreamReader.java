package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link InboundBodyPart} stream reader.
 */
public final class BodyPartStreamReader implements Reader<InboundBodyPart> {

    BodyPartStreamReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext ctx) {
        return BodyPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends InboundBodyPart> Publisher<U> read(
            Publisher<DataChunk> publisher, GenericType<U> type,
            ReaderContext context) {

        String boundary = null;
        MediaType contentType = context.contentType().orElse(null);
        if (contentType != null) {
            boundary = contentType.parameters().get("boundary");
        }
        if (boundary == null) {
            throw new IllegalStateException("boudary header is missing");
        }
        MultiPartDecoder decoder = MultiPartDecoder.create(boundary, context);
        publisher.subscribe(decoder);
        return (Publisher<U>) decoder;
    }

    public static BodyPartStreamReader create() {
        return new BodyPartStreamReader();
    }
}
