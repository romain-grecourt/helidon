/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.media.multipart;

import io.helidon.common.http.ContentInfo;
import io.helidon.common.http.ContentInterceptor;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.InBoundContext;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Subscriber;
import java.nio.charset.Charset;

/**
 * In-bound body part content context.
 */
final class InBoundBodyPartContentContext implements InBoundContext {

    private final InBoundContext delegate;
    private final ContentInfo contentInfo;

    InBoundBodyPartContentContext(InBoundContext delegate,
            MediaType contentType) {

        this.delegate = delegate;
        this.contentInfo = new ContentInfo(contentType);
    }

    @Override
    public ContentInfo contentInfo() {
        return contentInfo;
    }

    @Override
    public Charset defaultCharset() {
        return delegate.defaultCharset();
    }

    @Override
    public ContentInterceptor createInterceptor(
            Subscriber<? super DataChunk> subscriber, String type) {

        return null;
    }
}
