package io.helidon.media.jsonb.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.media.common.CharBuffer;
import io.helidon.media.common.CharBufferEntityWriter;
import io.helidon.media.common.JsonHelper;
import java.util.Objects;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;

/**
 * JSON-B entity writer.
 */
public class JsonBindingEntityWriter implements EntityWriter<Object> {

    private final Jsonb jsonb;

    public JsonBindingEntityWriter(Jsonb jsonb) {
        Objects.requireNonNull(jsonb);
        this.jsonb = jsonb;
    }

    @Override
    public Promise accept(Object entity, OutBoundScope scope) {
        if (entity != null && !(entity instanceof CharSequence)) {
            MediaType contentType = JsonHelper.getOutBoundContentType(scope);
            if (contentType != null) {
                return new Promise<>(this, contentType);
            }
        }
        return null;
    }

    @Override
    public Publisher<DataChunk> writeEntity(Object entity,
            Promise<Object> promise, OutBoundScope scope) {

        CharBuffer buffer = new CharBuffer();
        try {
            jsonb.toJson(entity, buffer);
            return CharBufferEntityWriter.write(buffer, scope.charset());
        } catch (IllegalStateException | JsonbException ex) {
            return new FailedPublisher<>(ex);
        }
    }
}
