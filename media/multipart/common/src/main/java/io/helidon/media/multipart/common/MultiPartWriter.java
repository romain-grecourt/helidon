package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.FixedItemsPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleInputDelegatingProcessor;

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
    public <U extends OutboundMultiPart> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        context.contentType(MediaType.MULTIPART_FORM_DATA);
        MultiPartEncoder encoder = MultiPartEncoder.create(boundary, context);
        Processor processor = new Processor(encoder);
        return processor;
    }

    public static MultiPartWriter create(String boundary) {
        return new MultiPartWriter(boundary);
    }

    public static MultiPartWriter create() {
        return new MultiPartWriter(DEFAULT_BOUNDARY);
    }

    private static final class Processor
            extends SingleInputDelegatingProcessor<OutboundMultiPart, DataChunk> {

        private final MultiPartEncoder encoder;

        Processor(MultiPartEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        protected Publisher<DataChunk> delegate(OutboundMultiPart multiPart) {
            new FixedItemsPublisher<>(multiPart.bodyParts()).subscribe(encoder);
            return encoder;
        }
    }
}
