package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import io.helidon.media.common.ByteArrayWriter;
import io.helidon.common.reactive.SingleOutputProcessor;
import java.util.LinkedList;
import io.helidon.common.http.MessageBody.ReadableContent;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.http.MessageBodyReadableContent;

/**
 * {@link InboundMultiPart} reader.
 */
public final class MultiPartReader implements Reader<MultiPart> {

    MultiPartReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext ctx) {
        return MultiPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends MultiPart> Publisher<U> read(Publisher<DataChunk> publisher, GenericType<U> type, ReaderContext context) {
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
        Processor processor = new Processor();
        decoder.subscribe(processor);
        return (Publisher<U>) processor;
    }

    private static final class Processor
            extends SingleOutputProcessor<MultiPart, InboundBodyPart> {

        private final LinkedList<InboundBodyPart> bodyParts = new LinkedList<>();

        @Override
        public void onNext(InboundBodyPart bodyPart){
            MessageBodyReadableContent content = MessageBodyReadableContent
                    .of(bodyPart.content());

            // buffer the data
            Publisher<DataChunk> bufferedData = ByteArrayWriter
                    .write(ByteArrayReader.read(content),
                            /* copy */ true);

            // create a content copy with the buffered data
            ReadableContent contentCopy = MessageBodyReadableContent
                    .create(bufferedData, content.context());

            // create a new body part with the buffered content
            InboundBodyPart bufferedBodyPart = InboundBodyPart.builder()
                    .headers(bodyPart.headers())
                    .content(contentCopy)
                    .buffered()
                    .build();
            bodyParts.add(bufferedBodyPart);
        }

        @Override
        public void onComplete() {
            submit(new InboundMultiPart(bodyParts));
        }
    }

    public static MultiPartReader create() {
        return new MultiPartReader();
    }
}
