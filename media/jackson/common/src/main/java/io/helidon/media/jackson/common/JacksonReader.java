package io.helidon.media.jackson.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MessageBody.Reader;
import io.helidon.common.http.MessageBody.ReaderContext;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.ByteArrayReader;
import io.helidon.common.reactive.SingleOutputProcessor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Jackson content reader.
 */
public final class JacksonReader implements Reader<Object> {

    private final ObjectMapper objectMapper;

    public JacksonReader(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean accept(GenericType<?> type, ReaderContext context) {
        return !CharSequence.class.isAssignableFrom(type.rawType())
                && objectMapper.canDeserialize(
                        objectMapper.constructType(type));
    }

    @Override
    public <U extends Object> Publisher<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, ReaderContext context) {

        Processor<U> processor = new Processor<>(type, objectMapper);
        ByteArrayReader.read(publisher).subscribe(processor);
        return processor;
    }

    private static final class Processor<T>
            extends SingleOutputProcessor<T, ByteArrayOutputStream> {

        private final GenericType<? super T> type;
        private final ObjectMapper objectMapper;

        Processor(GenericType<? super T> type, ObjectMapper objectMapper) {
            this.type = type;
            this.objectMapper = objectMapper;
        }

        @Override
        public void onNext(ByteArrayOutputStream bytes) {
            try {
                submit(objectMapper.readValue(bytes.toByteArray(),
                        (Class<T>) type.rawType()));
            } catch (final IOException wrapMe) {
                error(new JacksonRuntimeException(wrapMe.getMessage(), wrapMe));
            }
        }
    }
}
