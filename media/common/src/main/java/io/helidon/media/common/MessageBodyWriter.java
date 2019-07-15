package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;

/**
 * Conversion operator that generate HTTP payload from objects.
 *
 * @param <T> type or base type supported by the operator
 */
public interface MessageBodyWriter<T>
        extends MessageBodyOperator<MessageBodyWriterContext> {

    /**
     * Generate HTTP payload from the objects of the given type.
     *
     * @param mono object mono publisher to convert to payload
     * @param type requested type
     * @param context the context providing the headers abstraction
     * @return Publisher of objects
     */
    Publisher<DataChunk> write(Mono<T> mono, GenericType<? extends T> type,
            MessageBodyWriterContext context);
}
