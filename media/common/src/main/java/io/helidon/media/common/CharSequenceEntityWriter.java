package io.helidon.media.common;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.EmptyPublisher;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import java.nio.charset.Charset;
import java.util.List;


/**
 * Entity writer for {@code CharSequence}.
 */
public final class CharSequenceEntityWriter
        implements EntityWriter<CharSequence> {

    @Override
    public Promise accept(Object entity, List<MediaType> acceptedTypes) {
        if (CharSequence.class.isAssignableFrom(entity.getClass())){
            return new Promise<>(new ContentInfo(MediaType.TEXT_PLAIN), this);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(CharSequence cs,
            ContentInfo info, List<MediaType> acceptedTypes,
            Charset defaultCharset) {

        if (cs == null || cs.length() == 0) {
            return new EmptyPublisher<>();
        }
        try {
            Charset charset = info.charset(defaultCharset);
            DataChunk chunk = DataChunk.create(false, charset
                    .encode(cs.toString()));
            return new SingleItemPublisher<>(chunk);
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
