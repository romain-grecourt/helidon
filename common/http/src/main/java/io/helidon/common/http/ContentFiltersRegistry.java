package io.helidon.common.http;

/**
 * Content filters registry.
 */
public interface ContentFiltersRegistry {

    ContentFiltersRegistry registerFilter(ContentFilter filter);
}
