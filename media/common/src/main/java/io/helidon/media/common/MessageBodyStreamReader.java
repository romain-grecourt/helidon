package io.helidon.media.common;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Conversion operator that can convert HTTP payload into many objects.
 *
 * @param <T> type or base type supported by the operator
 */
public interface MessageBodyStreamReader<T>
        extends MessageBodyOperator<MessageBodyReaderContext> {

    /**
     * Convert a HTTP payload into objects of the given type.
     *
     * @param <U> actual requested type parameter
     * @param publisher HTTP payload
     * @param type requested type
     * @param context the context providing the headers abstraction
     * @return Publisher of objects
     */
    <U extends T> Publisher<U> read(Publisher<DataChunk> publisher,
            GenericType<U> type, MessageBodyReaderContext context);
}
