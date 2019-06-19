package io.helidon.common.http;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/**
 * In-bound scope.
 */
public class InBoundScope {

    public final ReadOnlyParameters headers;
    public final Charset defaultCharset;
    public final MediaType contentType;
    public final EntityReaders readers;

    public InBoundScope(ReadOnlyParameters headers, Charset defaultCharset,
            MediaType contentType, EntityReaders readers) {

        Objects.requireNonNull(headers, "headers cannot be null!");
        Objects.requireNonNull(defaultCharset, "defaultCharset cannot be null!");
        Objects.requireNonNull(readers, "readers cannot be null!");
        this.headers = headers;
        this.defaultCharset = defaultCharset;
        this.contentType = contentType;
        if (readers != null) {
            this.readers = new EntityReaders(readers);
        } else {
            this.readers = new EntityReaders();
        }
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
        return content.scope;
    }
}
