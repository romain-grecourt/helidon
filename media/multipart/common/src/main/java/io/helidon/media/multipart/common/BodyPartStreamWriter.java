/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.media.multipart.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * {@link OutboundBodyPart} stream writer.
 */
public final class BodyPartStreamWriter implements Writer<OutboundBodyPart> {

    private final String boundary;

    BodyPartStreamWriter(String boundary) {
        this.boundary = boundary;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext ctx) {
        return OutboundBodyPart.class.isAssignableFrom(type.rawType());
    }

    @Override
    public <U extends OutboundBodyPart> Publisher<DataChunk> write(
            Publisher<U> content, GenericType<U> type, WriterContext context) {

        context.contentType(MediaType.MULTIPART_FORM_DATA);
        MultiPartEncoder encoder = MultiPartEncoder.create(boundary, context);
        content.subscribe(encoder);
        return encoder;
    }

    public static BodyPartStreamWriter create() {
        return new BodyPartStreamWriter(MultiPartWriter.DEFAULT_BOUNDARY);
    }

    public static BodyPartStreamWriter create(String boundary) {
        return new BodyPartStreamWriter(boundary);
    }
}
