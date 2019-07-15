package io.helidon.media.common;

import io.helidon.common.GenericType;

/**
 * Conversion operator that can be selected based on a requested type and a
 * message body context.
 *
 * @param <T> Type supported by the operator
 */
public interface MessageBodyOperator<T extends MessageBodyContext> {

    /**
     * Test if the operator can convert the given type.
     *
     * @param type the requested type
     * @param context the context providing the headers abstraction
     * @return {@code true} if the operator can convert the specified type in
     * the given context, {@code false} otherwise
     */
    boolean accept(GenericType<?> type, T context);
}
