package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import io.helidon.media.common.ByteArrayWriter;
import java.util.LinkedList;
import io.helidon.common.http.MessageBody.ReadableContent;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.http.MessageBodyReadableContent;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Mono;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link InboundMultiPart} reader.
 */
public final class MultiPartReader implements Reader<MultiPart> {

    /**
     * No public constructor.
     */
    MultiPartReader() {
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext ctx) {
        return MultiPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends MultiPart> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

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
        return (Mono<U>) Multi.from(decoder)
                .collect(LIST_SUPPLIER, new Collector())
                .flatMap(new Mapper());
    }

    /**
     * A mapper that creates that maps the list of buffered
     * {@link InboundBodyPart} to a {@link Mono} of {@link InboundMultiPart}.
     */
    private static final class Mapper
            implements Function<List<InboundBodyPart>, Mono<InboundMultiPart>> {

        @Override
        public Mono<InboundMultiPart> apply(List<InboundBodyPart> bodyParts) {
            return Mono.just(new InboundMultiPart(bodyParts));
        }
    }

    private static final Supplier<List<InboundBodyPart>> LIST_SUPPLIER =
            LinkedList<InboundBodyPart>::new;

    /**
     * A collector that accumulates and buffers body parts.
     */
    private static final class Collector
            implements BiConsumer<List<InboundBodyPart>, InboundBodyPart> {

        @Override
        public void accept(List<InboundBodyPart> bodyParts,
                InboundBodyPart bodyPart) {

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
    }

    /**
     * Create a new {@link MultiPartReader} instance.
     * @return MultiPartReader
     */
    public static MultiPartReader create() {
        return new MultiPartReader();
    }
}
