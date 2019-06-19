package io.helidon.media.common;

import io.helidon.common.http.MediaType;
import io.helidon.common.http.OutBoundScope;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author rgrecour
 */
public abstract class JsonHelper {

    private JsonHelper() {
    }

    /**
     * Determines if JSON is an accepted media type, using in-bound accepted
     * types and out-bound content type.
     *
     * @param scope out-bound scope
     * @return {@code MediaType} if JSON is accepted or {@code null} if not
     */
    public static MediaType getOutBoundContentType(OutBoundScope scope) {
        Objects.requireNonNull(scope, "scope cannot be null!");
        MediaType contentType = scope.contentType();
        if (contentType == null) {
            final MediaType acceptedType = findAccepted(scope.acceptedTypes);
            if (acceptedType == null) {
                return null;
            } else {
                return acceptedType;
            }
        } else {
            if (MediaType.JSON_PREDICATE.test(contentType)) {
                return contentType;
            } else {
                return null;
            }
        }
    }

    /**
     * Find a JSON compatible media type in the specified accepted media types.
     *
     * @param acceptedTypes accepted media types, may be {@code null}
     * @return the accepted media type, defaults to
     * {@link MediaType.APPLICATION_JSON} if acceptedTypes is {@code null} or
     * empty
     */
    private static MediaType findAccepted(List<MediaType> acceptedTypes) {
        if (acceptedTypes == null || acceptedTypes.isEmpty()) {
            // None provided, so go ahead and return JSON.
            return MediaType.APPLICATION_JSON;
        } else {
            for (final MediaType type : acceptedTypes) {
                final MediaType responseType = toJsonMediaType(type);
                if (responseType != null) {
                    return responseType;
                }
            }
            return null;
        }
    }

    /**
     * Returns the response type for the given type if it is an accepted JSON
     * type.
     *
     * @param acceptedType The accepted type.
     * @return The media type or {@code null} if not an accepted JSON type.
     */
    private static MediaType toJsonMediaType(MediaType acceptedType) {
        if (acceptedType.test(MediaType.APPLICATION_JSON)) {
            return MediaType.APPLICATION_JSON;
        } else if (MediaType.JSON_PREDICATE.test(acceptedType)) {
            return MediaType.create(acceptedType.type(), acceptedType.subtype());
        } else {
            return null;
        }
    }
}
