package io.helidon.media.common;

/**
 * Registry of {@link Writer} allowing to register writer instances in the
 * system.
 */
public interface MessageBodyWriters {

    /**
     * Register a writer.
     *
     * @param writer writer to register
     * @return Writers
     */
    MessageBodyWriters registerWriter(MessageBodyWriter<?> writer);

    /**
     * Register a stream writer.
     *
     * @param writer writer to register
     * @return Writers
     */
    MessageBodyWriters registerWriter(MessageBodyStreamWriter<?> writer);
}
