package io.helidon.webserver.examples.tutorial;

import io.helidon.common.GenericType;
import io.helidon.common.http.WritableHeaders;
import io.helidon.nima.http.media.MediaSupport;

final class CommentSupport implements MediaSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> WriterResponse<T> writer(GenericType<T> type, WritableHeaders<?> requestHeaders) {
        if (!type.equals(Comment.LIST_TYPE)) {
            return WriterResponse.unsupported();
        }
        return (WriterResponse<T>) new WriterResponse<>(SupportLevel.SUPPORTED, CommentWriter::new);
    }
}
