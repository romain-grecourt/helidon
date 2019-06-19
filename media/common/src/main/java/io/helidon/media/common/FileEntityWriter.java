package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import static io.helidon.media.common.ByteChannelEntityWriter.DEFAULT_RETRY_SCHEMA;

/**
 * Entity writer for {@link File}.
 */
public final class FileEntityWriter implements EntityWriter<File> {

    @Override
    public Promise accept(Object entity, OutBoundScope scope) {
        if (File.class.isAssignableFrom(entity.getClass())) {
            File file = (File) entity;
            return new Promise<>(this, MediaType.APPLICATION_OCTET_STREAM,
                    file.getTotalSpace());
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(File file, Promise<File> promise,
            OutBoundScope scope) {

        try {
            FileChannel fc = FileChannel.open(file.toPath(),
                    StandardOpenOption.READ);
            return new ReadableByteChannelPublisher(fc, DEFAULT_RETRY_SCHEMA);
        } catch (IOException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
