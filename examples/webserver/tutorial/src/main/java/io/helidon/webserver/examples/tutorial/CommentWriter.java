package io.helidon.webserver.examples.tutorial;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.common.GenericType;
import io.helidon.common.http.Headers;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.http.WritableHeaders;
import io.helidon.nima.http.media.EntityWriter;

final class CommentWriter implements EntityWriter<List<Comment>> {

    @Override
    public void write(GenericType<List<Comment>> type,
                      List<Comment> comments,
                      OutputStream os,
                      Headers requestHeaders,
                      WritableHeaders<?> responseHeaders) {

        write(comments, os, responseHeaders);
    }

    @Override
    public void write(GenericType<List<Comment>> type,
                      List<Comment> comments,
                      OutputStream os,
                      WritableHeaders<?> headers) {

        write(comments, os, headers);
    }

    private void write(List<Comment> comments, OutputStream os, Headers headers) {
        String str = comments.stream()
                             .map(Comment::toString)
                             .collect(Collectors.joining("\n"));

        Charset charset = headers.contentType()
                                 .flatMap(HttpMediaType::charset)
                                 .map(Charset::forName)
                                 .orElse(StandardCharsets.UTF_8);

        try {
            os.write(str.getBytes(charset));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
