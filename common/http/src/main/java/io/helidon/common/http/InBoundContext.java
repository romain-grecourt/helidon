package io.helidon.common.http;

import java.nio.charset.Charset;

/**
 * Contextual operations for in-bound media processing.
 */
public interface InBoundContext extends ContentInterceptor.Factory {

    ContentInfo contentInfo();

    Charset defaultCharset();

}
