package io.helidon.media.common;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;
import io.helidon.common.reactive.RetrySchema;

import static io.helidon.media.common.ByteChannelBodyWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Message body writer for {@link File}.
 */
public final class FileBodyWriter implements MessageBodyWriter<File> {

    /**
     * Singleton instance.
     */
    private static final FileBodyWriter INSTANCE = new FileBodyWriter();

    /**
     * Enforces the use of {@link #get()}.
     */
    private FileBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return File.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<File> content,
            GenericType<? extends File> type,
            MessageBodyWriterContext context) {

        return content.mapMany(new FileToChunks(DEFAULT_RETRY_SCHEMA,
                context));
    }

    /**
     * Get the {@link FileBodyWriter} singleton.
     * @return FileBodyWriter
     */
    public static FileBodyWriter get() {
        return INSTANCE;
    }

    /**
     * Implementation of {@link MultiMapper} that converts {@link File} to a
     * publisher of {@link DataChunk}.
     */
    private static final class FileToChunks
            implements MultiMapper<File, DataChunk> {

        private final RetrySchema schema;
        private final MessageBodyWriterContext context;

        FileToChunks(RetrySchema schema,
                MessageBodyWriterContext context) {

            this.schema = schema;
            this.context = context;
        }

        @Override
        public Publisher<DataChunk> map(File file) {
            try {
                Path path = file.toPath();
                context.contentType(MediaType.APPLICATION_OCTET_STREAM);
                context.contentLength(Files.size(path));
                FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
                return new ReadableByteChannelPublisher(fc, schema);
            } catch (IOException ex) {
                return Mono.<DataChunk>error(ex);
            }
        }
    }
}
