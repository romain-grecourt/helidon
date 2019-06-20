package io.helidon.media.multipart.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link OutBoundMultiPart} entity writer.
 */
public class MultiPartEntityWriter implements EntityWriter<OutBoundMultiPart> {

    /**
     * The default boundary used for encoding multipart messages.
     */
    public static final String DEFAULT_BOUNDARY = "[^._.^]==>boundary<==[^._.^]";

    private final String boundary;

    public MultiPartEntityWriter(String boundary) {
        this.boundary = boundary;
    }

    public MultiPartEntityWriter() {
        this.boundary = DEFAULT_BOUNDARY;
    }

    @Override
    public Ack<OutBoundMultiPart> accept(Object entity, OutBoundScope scope) {
        if (OutBoundMultiPart.class.isAssignableFrom(entity.getClass())) {
            return new Ack<>(this, MediaType.MULTIPART_FORM_DATA);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(OutBoundMultiPart multiPart,
            Ack<OutBoundMultiPart> ack, OutBoundScope scope) {

        MultiPartEncoder encoder = new MultiPartEncoder(boundary,
                scope.writers());
        new BodyPartPublisher<>(multiPart.bodyParts()).subscribe(encoder);
        return encoder;
    }
}
