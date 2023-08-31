package io.helidon.openapi.spi;

import java.util.List;
import java.util.function.BiConsumer;

import io.helidon.common.media.type.MediaType;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * OpenApi UI provider.
 */
public interface OpenApiUiProvider {

    /**
     * Indicates the media types the UI implementation itself supports.
     *
     * @return the supported media types
     */
    List<MediaType> supportedMediaTypes();

    /**
     * Get the UI handler.
     * @return handler
     */
    BiConsumer<ServerRequest, ServerResponse> handler();
}
