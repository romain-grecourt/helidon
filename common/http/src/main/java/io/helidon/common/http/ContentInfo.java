package io.helidon.common.http;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Content information.
 */
public final class ContentInfo {

    public final MediaType contentType;
    public final long contentLength;

    public ContentInfo(MediaType contentType, long contentLength) {
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public ContentInfo(MediaType contentType) {
        this.contentType = contentType;
        this.contentLength = -1;
    }

    public Charset charset(Charset defaultCharset)
            throws IllegalStateException {

        try {
            Charset charset = contentType.charset().map(Charset::forName)
                .orElse(defaultCharset);
            if (charset != null) {
                return charset;
            }
        } catch(IllegalCharsetNameException
                | UnsupportedCharsetException ex) {
            throw new IllegalStateException("Unable to derive charset", ex);
        }
        throw new IllegalStateException("defaultCharset cannot be null");
    }
}
