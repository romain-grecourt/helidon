package io.helidon.openapi;

import io.helidon.common.config.NamedService;

/**
 * OpenApi manager.
 *
 * @param <T> model type
 */
public interface OpenApiManager<T> extends NamedService {

    /**
     * Load the model.
     *
     * @param content initial static content, may be empty
     * @return in-memory model
     */
    T load(String content);

    /**
     * Format the model.
     *
     * @param model  model
     * @param format desired format
     * @return formatted content
     */
    String format(T model, OpenApiFormat format);
}
