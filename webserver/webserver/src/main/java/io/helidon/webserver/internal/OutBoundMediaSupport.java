package io.helidon.webserver.internal;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.StreamWriter;
import io.helidon.common.http.Writer;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import static io.helidon.common.reactive.ReactiveStreamsAdapter.publisherToFlow;
import io.helidon.media.common.ContentWriters;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

/**
 * Out-bound media support.
 */
public final class OutBoundMediaSupport extends MediaSupport {

    private final PredicateRegistry<MediaWriter<?>, MarshallInput<Object>> writers;
    private final PredicateRegistry<MediaStreamWriter<?>, MarshallInput<Class>> streamWriters;
    private final OutBoundContext context;

    public OutBoundMediaSupport(OutBoundContext context) {
        this.writers = new PredicateRegistry<>();
        this.streamWriters = new PredicateRegistry<>();
        this.context = context;
    }

    public OutBoundContext context() {
        return context;
    }

    @SuppressWarnings("unchecked")
    public <T> void registerWriter(Class<T> type, MediaType contentType,
            Writer<T> writer) {

        writers.register(new MediaWriter<>(new ObjectPredicate(type),
                contentType, writer));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerWriter(Predicate<?> predicate,
            MediaType contentType, Writer<T> writer) {

        writers.register(new MediaWriter(predicate, contentType, writer));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerStreamWriter(Class<T> type, MediaType contentType,
            StreamWriter<T> streamWriter) {

        streamWriters.register(new MediaStreamWriter<>(new TypePredicate(type),
                contentType, streamWriter));
    }

    @SuppressWarnings("unchecked")
    public <T> void registerStreamWriter(Predicate<Class<T>> predicate,
            MediaType contentType, StreamWriter<T> streamWriter) {

        streamWriters.register(new MediaStreamWriter(predicate, contentType,
                streamWriter));
    }

    public <T extends Object> Publisher<DataChunk> marshall(T content) {
        return marshall(content, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Object> Publisher<DataChunk> marshall(T content,
            MediaType contentType) {

        if (content == null) {
            return publisherToFlow(Mono.empty());
        }

        // Try to get a publisher from registered writers
        MediaWriter<T> writer = (MediaWriter<T>) writers
                .select(new MarshallInput<>(content, contentType));
        if (writer != null) {
            if (contentType == null && writer.contentType != null) {
                context.setContentType(writer.contentType);
            }
            return writer.apply(content);
        }
        return createDefaultPublisher(content, contentType);
    }

    @SuppressWarnings("unchecked")
    public <T extends Object> Publisher<DataChunk> marshallStream(
            Publisher<T> content, Class<T> itemClass, MediaType contentType) {

        if (content == null) {
            return publisherToFlow(Mono.empty());
        }

        // Try to get a publisher from registered writers
        MediaStreamWriter<T> writer = (MediaStreamWriter<T>) streamWriters
                .select(new MarshallInput(itemClass, contentType));
        if (writer != null) {
            if (contentType == null && writer.contentType != null) {
                context.setContentType(writer.contentType);
            }
            return writer.apply(content);
        }
        return null;
    }

    private <T> Publisher<DataChunk> createDefaultPublisher(T content,
            MediaType contentType) {

        final Class<?> type = content.getClass();
        if (File.class.isAssignableFrom(type)) {
            return toPublisher(((File) content).toPath());
        } else if (Path.class.isAssignableFrom(type)) {
            return toPublisher((Path) content);
        } else if (ReadableByteChannel.class.isAssignableFrom(type)) {
            return ContentWriters.byteChannelWriter()
                    .apply((ReadableByteChannel) content);
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return toPublisher((CharSequence) content, contentType);
        } else if (byte[].class.isAssignableFrom(type)) {
            return ContentWriters.byteArrayWriter(true)
                    .apply((byte[]) content);
        }
        return null;
    }

    private Flow.Publisher<DataChunk> toPublisher(CharSequence str,
            MediaType contentType) {

        Charset charset;
        if (contentType != null) {
            charset = contentType.charset()
                    .map(Charset::forName)
                    .orElse(StandardCharsets.UTF_8);
        } else {
            context.setContentType(MediaType.TEXT_PLAIN);
            charset = StandardCharsets.UTF_8;
        }
        return ContentWriters.charSequenceWriter(charset).apply(str);
    }

    private Flow.Publisher<DataChunk> toPublisher(Path path) {
        // Set response length - if possible
        try {
            // Is it existing and readable file
            if (!Files.exists(path)) {
                throw new IllegalArgumentException(
                        "File path argument doesn't exist!");
            }
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException(
                        "File path argument isn't a file!");
            }
            if (!Files.isReadable(path)) {
                throw new IllegalArgumentException(
                        "File path argument isn't readable!");
            }
            // Try to write length
            try {
                context.setContentLength(Files.size(path));
            } catch (Exception e) {
                // Cannot get length or write length, not a big deal
            }
            // And write
            FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);
            return ContentWriters.byteChannelWriter().apply(fc);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read a file!", e);
        }
    }

    /**
     * A predicate of object that tests if a given object has a type that can be
     * assigned from a specific class.
     * If the class associated with this
     * predicate is {@code null} the predicate always matches.
     */
    private static final class ObjectPredicate
            implements Predicate<Object> {

        private final Class<?> clazz;

        ObjectPredicate(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(Object o) {
            return clazz == null
                    || (o != null && clazz.isAssignableFrom(o.getClass()));
        }
    }

    /**
     * A predicate of class that tests if a given class can be assigned
     * from a specific class.
     * If the class associated with this predicate is {@code null} the predicate
     * always matches.
     */
    private static final class TypePredicate
            implements Predicate<Class> {

        private final Class<?> clazz;

        TypePredicate(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public boolean test(Class type) {
            return clazz == null
                    || (type != null && clazz.isAssignableFrom(type));
        }
    }

    /**
     * The input used to select a marshaller that combines a predicate
     * input (object or class) and a requested content type (optional).
     * @param <T> the type of the marshaller predicate input
     */
    private static final class MarshallInput<T> {

        private final T input;
        private final MediaType requestedContentType;

        MarshallInput(T input, MediaType contentType) {
            this.input = input;
            this.requestedContentType = contentType;
        }

        /**
         * Test if this input matches a given marshaller.
         * @param marshaller marshaller to test
         * @return {@code true} if this is a match, {@code false} otherwise
         */
        public boolean match(Marshaller<T> marshaller) {
            if (input == null || !marshaller.predicate.test(input)) {
                return false;
            }
            return requestedContentType == null
                    || requestedContentType.test(marshaller.contentType);
        }
    }

    /**
     * Marshaller consists of a predicate and a content type (optional) that
     * can be matched with {@link MarshallInput}.
     *
     * @param <T> the type of the predicate input
     */
    private static abstract class Marshaller<T>
            implements Predicate<MarshallInput<T>> {

        final Predicate<T> predicate;
        final MediaType contentType;

        protected Marshaller(Predicate<T> predicate, MediaType contentType) {
            Objects.requireNonNull(predicate, "predicate cannot be null");
            this.predicate = predicate;
            this.contentType = contentType;
        }

        @Override
        public boolean test(MarshallInput<T> input) {
            return input != null && input.match(this);
        }
    }

    /**
     * A media writer combines a writer with a predicate of object
     * and a content type.
     *
     * @param <T> type of the entity consumed by this writer
     */
    private static final class MediaWriter<T extends Object>
            extends Marshaller<Object> implements Writer<T> {

        final Writer<T> writer;

        /**
         * Create a new instance.
         *
         * @param predicate the predicate for this writer
         * @param contentType the content type compatible with this writer,
         * may be {@code null}
         * @param writer the underlying writer
         */
        MediaWriter(Predicate<Object> predicate, MediaType contentType,
                Writer<T> writer) {

            super(predicate, contentType);
            Objects.requireNonNull(writer, "writer cannot be null");
            this.writer = writer;
        }

        @Override
        public Publisher<DataChunk> apply(T entity) {
            return writer.apply(entity);
        }
    }

    /**
     * A media stream writer combines a stream writer with a predicate of object
     * and a content type.
     *
     * @param <T> type of the entity consumed by this writer
     */
    private static final class MediaStreamWriter<T> extends Marshaller<Class>
            implements StreamWriter<T> {

        private final StreamWriter<T> writer;

        /**
         * Create a new instance.
         *
         * @param predicate the predicate for this stream writer
         * @param contentType the content type compatible with this writer,
         * may be {@code null}
         * @param writer the underlying stream writer
         */
        public MediaStreamWriter(Predicate<Class> predicate,
                MediaType contentType, StreamWriter<T> writer) {

            super(predicate, contentType);
            Objects.requireNonNull(writer, "writer cannot be null");
            this.writer = writer;
        }

        @Override
        public Publisher<DataChunk> apply(Publisher<T> entityStream) {
            return writer.apply(entityStream);
        }
    }
}
