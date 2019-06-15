package io.helidon.common.http;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Contextual operations for out-bound media processing.
 */
public interface OutBoundContext extends ContentInterceptor.Factory {

    List<MediaType> acceptedTypes();

    Charset defaultCharset();

    void contentInfo(ContentInfo info);
}
