package io.helidon.common.http;

import io.helidon.common.CollectionsHelper;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;

/**
 * Out-bound scope.
 */
public class OutBoundScope {

    public final Parameters headers;
    public final Charset defaultCharset;
    public final List<MediaType> acceptedTypes;
    public final EntityWriters writers;

    public OutBoundScope(Parameters headers, Charset defaultCharset,
            List<MediaType> acceptedTypes, EntityWriters writers) {

        Objects.requireNonNull(headers, "headers cannot be null!");
        Objects.requireNonNull(defaultCharset, "defaultCharset cannot be null!");
        this.headers = headers;
        this.defaultCharset = defaultCharset;
        if(acceptedTypes != null) {
            this.acceptedTypes = acceptedTypes;
        } else {
            this.acceptedTypes = CollectionsHelper.listOf();
        }
        if (writers != null) {
            this.writers = new EntityWriters(writers);
        } else {
            this.writers = new EntityWriters();
        }
    }

    public OutBoundScope(Parameters headers, Charset defaultCharset) {
        this(headers, defaultCharset, /* acceptedTypes */ null,
                /* writers */ null);
    }

    public final MediaType contentType() {
        return headers.first(Http.Header.CONTENT_TYPE).map(MediaType::parse)
                .orElse(null);
    }

    public final Charset charset() throws IllegalStateException {
        MediaType contentType = contentType();
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

    public static OutBoundScope of(OutBoundContent content) {
        return content.scope;
    }
}
