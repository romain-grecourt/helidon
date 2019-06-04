package io.helidon.webserver.internal;

import io.opentracing.Tracer;
import java.nio.charset.Charset;

/**
 * Contextual operations for in-bound media processing.
 */
public interface InBoundContext {

    /**
     * Create a tracing span.
     *
     * @param operationName operation name for the created span
     * @return created span
     */
    Tracer.SpanBuilder createSpanBuilder(String operationName);

    /**
     * Get the charset to use when reading the content as a String.
     *
     * @return charset
     */
    Charset charset();
}
