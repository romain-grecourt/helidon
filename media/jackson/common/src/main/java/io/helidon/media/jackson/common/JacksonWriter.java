package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MessageBody.Writer;
import io.helidon.common.http.MessageBody.WriterContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.SingleItemPublisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * Jackson content writer.
 */
public final class JacksonWriter implements Writer<Object> {

    private final ObjectMapper objectMapper;

    public JacksonWriter(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type, WriterContext context) {
        return ! CharSequence.class.isAssignableFrom(type.rawType())
                && objectMapper.canSerialize(type.rawType());
    }

    @Override
    public <U> Publisher<DataChunk> write(Publisher<U> content,
            GenericType<U> type, WriterContext context) {

        MediaType contentType = context.findAccepted(MediaType.JSON_PREDICATE,
                MediaType.APPLICATION_JSON);
        context.contentType(contentType);
        try {
            CharBuffer buffer = new CharBuffer();
            objectMapper.writeValue(buffer, content);
            return CharBufferWriter.write(
                    new SingleItemPublisher<>(buffer), context.charset());
        } catch (IOException wrapMe) {
            throw new JacksonRuntimeException(wrapMe.getMessage(), wrapMe);
        }
    }
}
