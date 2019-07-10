package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import java.util.function.Function;

/**
 * {@link OutboundMultiPart} writer.
 */
public final class MultiPartWriter implements Writer<OutboundMultiPart> {

    /**
     * The default boundary used for encoding multipart messages.
     */
    public static final String DEFAULT_BOUNDARY = "[^._.^]==>boundary<==[^._.^]";

    private final String boundary;

    MultiPartWriter(String boundary) {
        this.boundary = boundary;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return OutboundMultiPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<OutboundMultiPart> content,
            GenericType<? extends OutboundMultiPart> type, WriterContext context) {

        context.contentType(MediaType.MULTIPART_FORM_DATA);
        return content.flatMapMany(new Mapper(boundary, context));
    }

    public static MultiPartWriter create(String boundary) {
        return new MultiPartWriter(boundary);
    }

    public static MultiPartWriter create() {
        return new MultiPartWriter(DEFAULT_BOUNDARY);
    }

    private static final class Mapper
            implements Function<OutboundMultiPart, Publisher<DataChunk>> {

        private final MultiPartEncoder encoder;

        Mapper(String boundary, WriterContext context) {
            this.encoder = MultiPartEncoder.create(boundary, context);
        }

        @Override
        public Publisher<DataChunk> apply(OutboundMultiPart multiPart) {
            Multi.just(multiPart.bodyParts()).subscribe(encoder);
            return encoder;
        }
    }
}
