package io.helidon.webserver.internal;

import io.helidon.common.http.MediaType;

/**
 * Contextual operations for out-bound media processing.
 */
public interface OutBoundContext {

    /**
     * Set the content type from the chosen writer.
     * @param mediaType contentType associated with the chosen writer
     */
    void setContentType(MediaType mediaType);

    /**
     * Set the content length.
     * @param size content size
     */
    void setContentLength(long size);
}
