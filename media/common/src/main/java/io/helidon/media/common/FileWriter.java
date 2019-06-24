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

import static io.helidon.media.common.ByteChannelWriter.DEFAULT_RETRY_SCHEMA;
import static io.helidon.media.common.PathEntityWriter.ANY;

/**
 * Entity writer for {@link File}.
 */
public final class FileWriter implements EntityWriter<File> {

    @Override
    public Ack accept(Object entity, Class<?> type, OutBoundScope scope) {
        if (File.class.isAssignableFrom(type)) {
            MediaType contentType = scope.findAccepted(ANY,
                    MediaType.APPLICATION_OCTET_STREAM);
            if (contentType != null) {
                File file = (File) entity;
                return new Ack(contentType, file.getTotalSpace());
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(File file, OutBoundScope scope) {
        try {
            FileChannel fc = FileChannel.open(file.toPath(),
                    StandardOpenOption.READ);
            return new ReadableByteChannelPublisher(fc, DEFAULT_RETRY_SCHEMA);
        } catch (IOException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
