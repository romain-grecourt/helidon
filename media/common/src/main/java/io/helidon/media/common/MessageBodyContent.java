package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;

/**
 * Reactive contract for a message body content.
 */
public interface MessageBodyContent extends Publisher<DataChunk> {
}
