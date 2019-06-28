package io.helidon.media.common;

import io.helidon.common.reactive.SingleInputDelegatingProcessor;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;
import java.nio.channels.ReadableByteChannel;

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
    public <U extends ReadableByteChannel> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        Processor processor = new Processor(schema, context);
        content.subscribe(processor);
        return processor;
    }

    public static ByteChannelWriter create(RetrySchema schema) {
        return new ByteChannelWriter(schema);
    }

    public static ByteChannelWriter create() {
        return new ByteChannelWriter(DEFAULT_RETRY_SCHEMA);
    }

    private static final class Processor
            extends SingleInputDelegatingProcessor<ReadableByteChannel, DataChunk> {

        private final RetrySchema schema;
        private final WriterContext context;

        Processor(RetrySchema schema, WriterContext context) {
            this.schema = schema;
            this.context = context;
        }

        @Override
        protected Publisher<DataChunk> delegate(ReadableByteChannel channel) {
            context.contentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ReadableByteChannelPublisher(channel, schema);
        }
    }
}
