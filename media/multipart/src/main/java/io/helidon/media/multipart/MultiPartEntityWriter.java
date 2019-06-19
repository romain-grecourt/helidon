package io.helidon.media.multipart;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link OutBoundMultiPart} entity writer.
 */
public class MultiPartEntityWriter implements EntityWriter<MultiPart> {

    private final String boundary;

    public MultiPartEntityWriter(String boundary) {
        this.boundary = boundary;
    }

    public MultiPartEntityWriter() {
        this.boundary = MultiPartSupport.DEFAULT_BOUNDARY;
    }

    @Override
    public Promise accept(Object entity, OutBoundScope scope) {
        if (OutBoundMultiPart.class.isAssignableFrom(entity.getClass())) {
            return new EntityWriter.Promise<>(this,
                    MediaType.MULTIPART_FORM_DATA);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(MultiPart multiPart,
            Promise<MultiPart> promise, OutBoundScope scope) {

        MultiPartEncoder encoder = new MultiPartEncoder(boundary,
                scope.writers);
        new MultiPartSupport.BodyPartPublisher<>(multiPart.bodyParts())
                .subscribe(encoder);
        return encoder;
    }
}
