package io.helidon.media.common;

import io.helidon.common.reactive.SingleInputDelegatingProcessor;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;

import static io.helidon.media.common.ByteChannelWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Writer for {@link File}.
 */
public final class FileWriter implements Writer<File> {

    private FileWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return File.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends File> Publisher<DataChunk> write(Publisher<U> content,
            GenericType<U> type, WriterContext context) {

        Processor processor = new Processor(DEFAULT_RETRY_SCHEMA, context);
        content.subscribe(processor);
        return processor;
    }

    public static FileWriter create() {
        return new FileWriter();
    }

    private static final class Processor
            extends SingleInputDelegatingProcessor<File, DataChunk> {

        private final RetrySchema schema;
        private final WriterContext context;

        Processor(RetrySchema schema, WriterContext context) {
            this.schema = schema;
            this.context = context;
        }

        @Override
        protected Publisher<DataChunk> delegate(File file) {
            try {
                Path path = file.toPath();
                context.contentType(MediaType.APPLICATION_OCTET_STREAM);
                context.contentLength(Files.size(path));
                FileChannel fc = FileChannel.open(path,
                        StandardOpenOption.READ);
                return new ReadableByteChannelPublisher(fc, schema);
            } catch (IOException ex) {
                return new FailedPublisher<>(ex);
            }
        }
    }
}
