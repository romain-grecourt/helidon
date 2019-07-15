package io.helidon.media.common;

/**
 * Registry of {@link MessageBodyFilters}.
 */
public interface MessageBodyFilters {

    /**
     * Register a message body filter.
     *
     * @param filter message body filter to register
     * @return MessageBodyFilters
     */
    MessageBodyFilters registerFilter(MessageBodyFilter filter);
}
