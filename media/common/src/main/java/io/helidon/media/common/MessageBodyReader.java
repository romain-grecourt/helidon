package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Mono;

/**
 * Conversion operator that can convert HTTP payload into one object.
 *
 * @param <T> type or base type supported by the operator
 */
public interface MessageBodyReader<T>
        extends MessageBodyOperator<MessageBodyReaderContext> {

    /**
     * Convert a HTTP payload into a Mono publisher of the given type.
     *
     * @param <U> actual requested type parameter
     * @param publisher HTTP payload
     * @param type requested type
     * @param context the context providing the headers abstraction
     * @return Mono publisher
     */
    <U extends T> Mono<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context);
}
