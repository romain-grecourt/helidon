package io.helidon.webserver.internal;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Reader;
import io.helidon.common.http.StreamReader;
import io.helidon.common.reactive.Flow.Publisher;
import java.io.InputStream;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import static io.helidon.media.common.ContentReaders.byteArrayReader;
import static io.helidon.media.common.ContentReaders.inputStreamReader;
import static io.helidon.media.common.ContentReaders.stringReader;

/**
 * In-bound media content support.
 */
public final class InBoundMediaSupport extends MediaSupport {

    private final PredicateRegistry<MediaReader<?>, Class> readers;
    private final PredicateRegistry<MediaStreamReader<?>, Class> streamReaders;
    private final InBoundContext context;

    /**
     * Create a new in-bound media support instance.
     * @param ctx in-bound context
     */
    public InBoundMediaSupport(InBoundContext ctx) {
        this.context = ctx;
        this.readers = new PredicateRegistry<>(defaultReaders());
        this.streamReaders = new PredicateRegistry<>();
    }

    /**
     * Get the in-bound context.
     * @return InBoundContext
     */
    public InBoundContext context() {
        return context;
    }

    @SuppressWarnings("unchecked")
    public <T> void registerStreamReader(Class<T> clazz,
            StreamReader<T> reader) {

        streamReaders.register(new MediaStreamReader(
                new TypePredicate(clazz),reader));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerStreamReader(Predicate<Class<T>> predicate,
            StreamReader<T> reader) {

        streamReaders.register(new MediaStreamReader(predicate, reader));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        readers.register(new MediaReader(new TypePredicate(type),
                reader));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerReader(Predicate<Class<T>> predicate,
            Reader<T> reader) {

        readers.register(new MediaReader(predicate, reader));
    }

    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> unmarshall(final Class<T> type,
            Publisher<DataChunk> publisher) {

        CompletionStage<T> result;
        try {
            MediaReader<T> reader = (MediaReader<T>) readers.select(type);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }
            result = (CompletionStage<T>) reader.apply(
                    applyFilters(publisher), type);
        } catch (IllegalArgumentException e) {
            result = failedFuture(e);
        } catch (Exception e) {
            result = failedFuture(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> Publisher<T> unmarshallStream(final Class<T> type,
            Publisher<DataChunk> publisher) {

        Publisher<T> result;
            MediaStreamReader<T> reader = (MediaStreamReader<T>) streamReaders
                    .select(type);
            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }
            result = (Publisher<T>) reader.apply(
                    applyFilters(publisher), type);
        return result;
    }

    /**
     * Create the default string reader, handle {@link UnsupportedCharsetException}
     * by returning a failed reader that delays throwing the exception when
     * {@link Reader#apply(Publisher)} is called.
     * @return default string reader
     */
    private Reader<String> stringContentReader() {
        try {
            return stringReader(context.charset());
        } catch (final UnsupportedCharsetException e) {
            return new FailedStringReader(e);
        }
    }

    /**
     * Create the default readers.
     * @param charset charset to use the string reader
     * @return list of {@code MediaReader}
     */
    @SuppressWarnings("unchecked")
    private LinkedList<MediaReader<?>> defaultReaders() {
        LinkedList<MediaReader<?>> defaultReaders = new LinkedList<>();
        defaultReaders.addLast(new MediaReader(
                new TypePredicate(String.class),
                stringContentReader()));
        defaultReaders.addLast(new MediaReader(
                new TypePredicate(byte[].class), byteArrayReader()));
        defaultReaders.addLast(new MediaReader(
                new TypePredicate(InputStream.class),
                inputStreamReader()));
        return defaultReaders;
    }

    /**
     * Utility method to create a failed future.
     * @param ex the exception for the failed future
     * @return CompletableFuture
     */
    private static CompletableFuture failedFuture(Throwable ex) {
        CompletableFuture result = new CompletableFuture<>();
        result.completeExceptionally(ex);
        return result;
    }

    /**
     * A failed {@code Reader<String>} implementation to delay throwing the
     * {@link UnsupportedCharsetException} when {@link #apply(Publisher)} is
     * called.
     */
    private static final class FailedStringReader implements Reader<String> {

        private final UnsupportedCharsetException ex;

        /**
         * Create a new failed string reader with the given exception.
         * @param ex unsupported charset exception
         */
        FailedStringReader(UnsupportedCharsetException ex) {
            this.ex = ex;
        }

        @Override
        public CompletionStage<? extends String> apply(
                Publisher<DataChunk> publisher, Class<? super String> clazz) {

            throw ex;
        }
    }

    /**
     * A static predicate of type.
     * @param <T> the type associated with this predicate
     */
    private static final class TypePredicate
            implements Predicate<Class<?>> {

        private final Class<?> clazz;

        TypePredicate(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(Class<?> aClass) {
            return aClass.isAssignableFrom(clazz);
        }
    }

    /**
     * A media reader combines a reader and a predicate.
     *
     * @param <T> type of the instances produced by this reader
     */
    private static final class MediaReader<T>
            implements Reader<T>, Predicate<Class> {

        private final Predicate<Class> predicate;
        private final Reader<T> reader;

        /**
         * Create a new instance.
         *
         * @param predicate the predicate for this reader
         * @param reader the underlying reader
         */
        MediaReader(Predicate<Class> predicate, Reader<T> reader) {
            this.predicate = predicate;
            this.reader = reader;
        }

        @Override
        public CompletionStage<? extends T> apply(
                Publisher<DataChunk> publisher, Class<? super T> clazz) {

            return reader.apply(publisher, clazz);
        }

        @Override
        public boolean test(Class o) {
            return o != null && predicate != null && predicate.test(o);
        }
    }

    /**
     * A media stream reader combines a stream reader and a predicate.
     *
     * @param <T> type of the instances produced by this reader
     */
    private final class MediaStreamReader<T>
            implements StreamReader<T>, Predicate<Class> {

        private final Predicate<Class> predicate;
        private final StreamReader<T> reader;

        /**
         * Create a new instance.
         *
         * @param predicate the predicate for this stream reader
         * @param reader the underlying stream reader
         */
        public MediaStreamReader(Predicate<Class> predicate,
                StreamReader<T> reader) {

            this.predicate = predicate;
            this.reader = reader;
        }

        @Override
        public Publisher<? extends T> apply(
                Publisher<DataChunk> publisher, Class<? super T> clazz) {

            return reader.apply(publisher, clazz);
        }

        @Override
        public boolean test(Class o) {
            return o != null && predicate != null && predicate.test(o);
        }
    }
}