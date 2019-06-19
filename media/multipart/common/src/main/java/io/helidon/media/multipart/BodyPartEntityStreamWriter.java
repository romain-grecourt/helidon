/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.media.multipart;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityStreamWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link OutBoundBodyPart} entity stream writer.
 */
public class BodyPartEntityStreamWriter
        implements EntityStreamWriter<OutBoundBodyPart> {

    private final String boundary;

    public BodyPartEntityStreamWriter(String boundary) {
        this.boundary = boundary;
    }

    public BodyPartEntityStreamWriter() {
        this.boundary = MultiPartSupport.DEFAULT_BOUNDARY;
    }

    @Override
    public EntityStreamWriter.Promise accept(Class<?> type,
            OutBoundScope scope) {

        if (OutBoundBodyPart.class.isAssignableFrom(type)) {
            return new EntityStreamWriter.Promise<>(this,
                    MediaType.MULTIPART_FORM_DATA);
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntityStream(
            Publisher<OutBoundBodyPart> parts, Class<OutBoundBodyPart> type,
            Promise<OutBoundBodyPart> promise, OutBoundScope scope) {

        MultiPartEncoder encoder = new MultiPartEncoder(boundary,
                scope.writers);
        parts.subscribe(encoder);
        return encoder;
    }
}
