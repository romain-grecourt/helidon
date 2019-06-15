package io.helidon.common.http;

/**
 * Content filters registry.
 */
public interface ContentFiltersRegistry {

    void registerFilter(ContentFilter filter);
}
