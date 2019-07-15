package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;
import io.helidon.common.reactive.MultiMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.helidon.media.common.ByteChannelBodyWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Message body writer for {@link Path}.
 */
public final class PathBodyWriter implements MessageBodyWriter<Path> {

    /**
     * Singleton instance.
     */
    private static final PathBodyWriter INSTANCE = new PathBodyWriter();

    /**
     * Enforces the use of {@link #get()}.
     */
    private PathBodyWriter() {
    }

    @Override
    public boolean accept(GenericType<?> type,
            MessageBodyWriterContext context) {

        return Path.class.isAssignableFrom(type.rawType());
    }

    @Override
    public Publisher<DataChunk> write(Mono<Path> content,
            GenericType<? extends Path> type,
            MessageBodyWriterContext context) {

        return content.mapMany(new PathToChunks(DEFAULT_RETRY_SCHEMA,
                context));
    }

    /**
     * Get the {@link PathBodyWriter} singleton.
     * @return 
     */
    public static PathBodyWriter get() {
        return INSTANCE;
    }

    /**
     * Implementation of {@link MultiMapper} that converts a {@link Path} to a
     * publisher of {@link DataChunk}.
     */
    private static final class PathToChunks
            implements MultiMapper<Path, DataChunk> {

        private final RetrySchema schema;
        private final MessageBodyWriterContext context;

        PathToChunks(RetrySchema schema,
                MessageBodyWriterContext context) {

            this.schema = schema;
            this.context = context;
        }

        @Override
        public Publisher<DataChunk> map(Path path) {
            try {
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
