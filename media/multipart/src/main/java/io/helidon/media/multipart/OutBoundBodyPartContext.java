package io.helidon.media.multipart;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.ContentInterceptor;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundContext;
import io.helidon.common.reactive.Flow.Subscriber;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Out-bound body part context.
 */
final class OutBoundBodyPartContext implements OutBoundContext {

    @Override
    public List<MediaType> acceptedTypes() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public void contentInfo(ContentInfo info) {
        // do nothing for now.
        // requires out-bound body part headers to be non read-only.
    }

    @Override
    public ContentInterceptor createInterceptor(
            Subscriber<? super DataChunk> subscriber, String type) {

        return null;
    }

}
