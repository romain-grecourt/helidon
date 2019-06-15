package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import static io.helidon.media.common.ByteChannelEntityWriter.DEFAULT_RETRY_SCHEMA;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Entity writer for {@link File}.
 */
public final class FileEntityWriter implements EntityWriter<File> {

    @Override
    public Promise accept(Object entity, List<MediaType> acceptedTypes) {
        if (File.class.isAssignableFrom(entity.getClass())) {
            File file = (File) entity;
            return new Promise<>(new ContentInfo(
                    MediaType.APPLICATION_OCTET_STREAM, file.getTotalSpace()),
                    this);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(File file, ContentInfo info,
            List<MediaType> acceptedTypes, Charset defaultCharset) {

        try {
            FileChannel fc = FileChannel.open(file.toPath(),
                    StandardOpenOption.READ);
            return new ReadableByteChannelPublisher(fc, DEFAULT_RETRY_SCHEMA);
        } catch (IOException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
