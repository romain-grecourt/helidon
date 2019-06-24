package io.helidon.common.http;

import io.helidon.common.CollectionsHelper;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Out-bound scope.
 */
public class OutBoundScope {

    private final Parameters headers;
    private final Charset defaultCharset;
    private final List<MediaType> acceptedTypes;
    private final EntityWriters writers;
    private boolean contentTypeCached;
    private MediaType contentTypeCache;
    private boolean charsetCached;
    private Charset charsetCache;

    public OutBoundScope(Parameters headers, Charset defaultCharset,
            List<MediaType> acceptedTypes, EntityWriters parentWriters) {

        Objects.requireNonNull(headers, "headers cannot be null!");
        Objects.requireNonNull(defaultCharset, "defaultCharset cannot be null!");
        this.headers = headers;
        this.defaultCharset = defaultCharset;
        if(acceptedTypes != null) {
            this.acceptedTypes = acceptedTypes;
        } else {
            this.acceptedTypes = CollectionsHelper.listOf();
        }
        if (parentWriters != null) {
            this.writers = new EntityWriters(parentWriters);
        } else {
            this.writers = new EntityWriters();
        }
    }

    public OutBoundScope(Parameters headers, Charset defaultCharset) {
        this(headers, defaultCharset, /* acceptedTypes */ null,
                /* writers */ null);
    }

    public EntityWriters writers() {
        return writers;
    }

    public List<MediaType> acceptedTypes() {
        return acceptedTypes;
    }

    public Charset defaultCharset() {
        return defaultCharset;
    }

    public Parameters headers() {
        return headers;
    }

    public final MediaType contentType() {
        if (contentTypeCached) {
            return contentTypeCache;
        }
        contentTypeCache = headers
                .first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .orElse(null);
        contentTypeCached = true;
        return contentTypeCache;
    }

    public MediaType findAccepted(Predicate<MediaType> predicate,
            MediaType defaultType) {

        Objects.requireNonNull(predicate, "predicate cannot be null");
        Objects.requireNonNull(defaultType, "defaultType cannot be null");
        MediaType contentType = contentType();
        if (contentType == null) {
            if (acceptedTypes.isEmpty()) {
                return defaultType;
            } else {
                for (final MediaType acceptedType : acceptedTypes) {
                    if (predicate.test(acceptedType)) {
                        if (acceptedType.isWildcardType()
                                || acceptedType.isWildcardSubtype()) {
                            return defaultType;
                        }
                        return MediaType.create(acceptedType.type(),
                                acceptedType.subtype());
                    }
                }
            }
        } else {
            if (predicate.test(contentType)) {
                return contentType;
            }
        }
        return null;
    }

    public MediaType findAccepted(MediaType mediaType) {
        Objects.requireNonNull(mediaType, "mediaType cannot be null");
        for (MediaType acceptedType : acceptedTypes) {
            if (mediaType.equals(acceptedType)) {
                return acceptedType;
            }
        }
        return null;
    }

    public final Charset charset() throws IllegalStateException {
        if (charsetCached) {
            return charsetCache;
        }
        MediaType contentType = contentType();
        if (contentType != null) {
            try {
                charsetCache = contentType.charset().map(Charset::forName)
                        .orElse(defaultCharset);
                charsetCached = true;
                return charsetCache;
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        charsetCache = defaultCharset;
        charsetCached = true;
        return charsetCache;
    }

    public static OutBoundScope of(OutBoundContent content) {
        return content.scope();
    }
}
