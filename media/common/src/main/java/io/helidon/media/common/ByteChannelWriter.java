package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Function;

/**
 * Writer for {@link ReadableByteChannel}.
 */
public final class ByteChannelWriter implements Writer<ReadableByteChannel> {

    static final RetrySchema DEFAULT_RETRY_SCHEMA =
            RetrySchema.linear(0, 10, 250);

    private final RetrySchema schema;

    private ByteChannelWriter(RetrySchema schema) {
        this.schema = schema;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return ReadableByteChannel.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<ReadableByteChannel> content,
            GenericType<? extends ReadableByteChannel> type,
            WriterContext context) {

        return content.flatMapMany(new Mapper(schema, context));
    }

    public static ByteChannelWriter create(RetrySchema schema) {
        return new ByteChannelWriter(schema);
    }

    public static ByteChannelWriter create() {
        return new ByteChannelWriter(DEFAULT_RETRY_SCHEMA);
    }

    private static final class Mapper
            implements Function<ReadableByteChannel, Publisher<DataChunk>> {

        private final RetrySchema schema;
        private final WriterContext context;

        Mapper(RetrySchema schema, WriterContext context) {
            this.schema = schema;
            this.context = context;
        }

        @Override
        public Publisher<DataChunk> apply(ReadableByteChannel channel) {
            context.contentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ReadableByteChannelPublisher(channel, schema);
        }
    }
}
