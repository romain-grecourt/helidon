package io.helidon.media.jsonp.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferEntityWriter;
import io.helidon.media.common.JsonHelper;
import java.nio.charset.Charset;
import javax.json.JsonException;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

/**
 * JSON-P entity writer.
 */
public class JsonEntityWriter implements EntityWriter<JsonStructure> {

    private final JsonWriterFactory jsonWriterFactory;

    public JsonEntityWriter(JsonWriterFactory jsonWriterFactory) {
        this.jsonWriterFactory = jsonWriterFactory;
    }

    @Override
    public Promise accept(Object entity, OutBoundScope scope) {
        if (entity != null && entity instanceof JsonStructure) {
            MediaType contentType = JsonHelper.getOutBoundContentType(scope);
            if (contentType != null) {
                return new Promise<>(this, contentType);
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(JsonStructure entity,
            Promise<JsonStructure> promise, OutBoundScope scope) {

        try {
            return write(jsonWriterFactory, entity,  scope.charset());
        } catch (IllegalStateException ex) {
            return new FailedPublisher<>(ex);
        }
    }

    static Publisher<DataChunk> write(JsonWriterFactory factory,
            JsonStructure entity, Charset charset) {

        CharBuffer buffer = new CharBuffer();
        try (JsonWriter writer = factory.createWriter(buffer)) {
            writer.write(entity);
            return CharBufferEntityWriter.write(buffer, charset);
        } catch (IllegalStateException | JsonException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
