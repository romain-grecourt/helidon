package io.helidon.common.http;

import io.helidon.common.reactive.Flow.Subscriber;

/**
 * Content helper.
 */
public final class ContentHelper {

    public static InBoundContext getInBoundContext(InBoundContent content) {
        return content.inBoundContext;
    }

    public static EntityReaders getReaders(InBoundContent content){
        return content.readers;
    }

    public static void subscribe(OutBoundContent content,
            Subscriber<DataChunk> subscriber, EntityWriters writers) {

        content.subscribe(subscriber, writers);
    }
}
