package io.helidon.media.common;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
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
    public Publisher<DataChunk> write(Mono<File> content,
            GenericType<? extends File> type, WriterContext context) {

        return content.flatMapMany(new Mapper(DEFAULT_RETRY_SCHEMA, context));
    }

    public static FileWriter create() {
        return new FileWriter();
    }

    private static final class Mapper
            implements Function<File, Publisher<DataChunk>> {

        private final RetrySchema schema;
        private final WriterContext context;

        Mapper(RetrySchema schema, WriterContext context) {
            this.schema = schema;
            this.context = context;
        }

        @Override
        public Publisher<DataChunk> apply(File file) {
            try {
                Path path = file.toPath();
                context.contentType(MediaType.APPLICATION_OCTET_STREAM);
                context.contentLength(Files.size(path));
                FileChannel fc = FileChannel.open(path,
                        StandardOpenOption.READ);
                return new ReadableByteChannelPublisher(fc, schema);
            } catch (IOException ex) {
                return Mono.<DataChunk>error(ex);
            }
        }
    }
}
