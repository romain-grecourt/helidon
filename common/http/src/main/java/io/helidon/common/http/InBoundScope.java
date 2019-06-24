package io.helidon.common.http;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/**
 * In-bound scope.
 */
public class InBoundScope {

    private final ReadOnlyParameters headers;
    private final Charset defaultCharset;
    private final MediaType contentType;
    private final EntityReaders readers;

    public InBoundScope(ReadOnlyParameters headers, Charset defaultCharset,
            MediaType contentType, EntityReaders parentReaders) {

        Objects.requireNonNull(headers, "headers cannot be null!");
        Objects.requireNonNull(defaultCharset, "defaultCharset cannot be null!");
        Objects.requireNonNull(parentReaders, "readers cannot be null!");
        this.headers = headers;
        this.defaultCharset = defaultCharset;
        this.contentType = contentType;
        if (parentReaders != null) {
            this.readers = new EntityReaders(parentReaders);
        } else {
            this.readers = new EntityReaders();
        }
    }

    public ReadOnlyParameters headers() {
        return headers;
    }

    public Charset defaultCharset() {
        return defaultCharset;
    }

    public MediaType contentType() {
        return contentType;
    }

    public EntityReaders readers() {
        return readers;
    }

    public final Charset charset() {
        if (contentType != null) {
            try {
                return contentType.charset().map(Charset::forName)
                        .orElse(defaultCharset);
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return defaultCharset;
    }

    public static InBoundScope of(InBoundContent content) {
        return content.scope();
    }
}
