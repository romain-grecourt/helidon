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

import static io.helidon.media.common.ByteChannelEntityWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Entity writer for {@link Path}.
 */
public final class PathEntityWriter implements EntityWriter<Path> {

    @Override
    public Promise accept(Object entity, OutBoundScope scope) {
        if (Path.class.isAssignableFrom(entity.getClass())) {
            Path path = (Path) entity;
            long size;
            try {
                size = Files.size(path);
            } catch (IOException ex) {
                size = 1;
            }
            return new Promise<>(this, MediaType.APPLICATION_OCTET_STREAM, size);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(Path path, Promise<Path> promise,
            OutBoundScope scope) {

        try {
            FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
            return new ReadableByteChannelPublisher(fc, DEFAULT_RETRY_SCHEMA);
        } catch (IOException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
