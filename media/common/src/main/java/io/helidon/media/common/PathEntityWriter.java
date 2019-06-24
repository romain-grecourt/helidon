package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

import static io.helidon.media.common.ByteChannelWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Entity writer for {@link Path}.
 */
public final class PathEntityWriter implements EntityWriter<Path> {

    static final Predicate<MediaType> ANY = new AnyPredicate();

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (Path.class.isAssignableFrom(type)) {
            MediaType contentType = scope.findAccepted(ANY,
                    MediaType.APPLICATION_OCTET_STREAM);
            if (contentType != null) {
                Path path = (Path) entity;
                long size;
                try {
                    size = Files.size(path);
                } catch (IOException ex) {
                    size = 1;
                }
                return new Ack(contentType, size);
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(Path path, OutBoundScope scope) {
        try {
            FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
            return new ReadableByteChannelPublisher(fc, DEFAULT_RETRY_SCHEMA);
        } catch (IOException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    /**
     * A media type predicate that accepts any type.
     */
    private static final class AnyPredicate implements Predicate<MediaType> {

        @Override
        public boolean test(MediaType t) {
            return true;
        }
    }
}
