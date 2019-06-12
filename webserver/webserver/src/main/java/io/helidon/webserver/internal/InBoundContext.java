package io.helidon.webserver.internal;

import java.nio.charset.Charset;

/**
 * Contextual operations for in-bound media processing.
 */
public interface InBoundContext {

    /**
     * Get the charset to use when reading the content as a String.
     *
     * @return charset
     */
    Charset charset();
}
