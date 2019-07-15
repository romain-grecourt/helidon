package io.helidon.media.common;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Processor;

/**
 * Reactive contract for processing message body content before conversion
 * (inbound payload) or after conversion (outbound payload).
 */
public interface MessageBodyFilter extends Processor<DataChunk, DataChunk> {
}
