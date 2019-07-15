package io.helidon.media.common;

/**
 * Registry of {@link Reader} allowing to register reader instances in the
 * system.
 */
public interface MessageBodyReaders {

    /**
     * Register a reader.
     *
     * @param reader reader to register
     * @return Readers
     */
    MessageBodyReaders registerReader(MessageBodyReader<?> reader);

    /**
     * Register a stream reader.
     *
     * @param reader reader to register
     * @return Readers
     */
    MessageBodyReaders registerReader(MessageBodyStreamReader<?> reader);
}
