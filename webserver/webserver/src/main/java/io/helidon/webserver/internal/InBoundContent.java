package io.helidon.webserver.internal;

import io.helidon.common.CollectionsHelper;
import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Filter;
import io.helidon.common.http.Reader;
import io.helidon.common.http.StreamReader;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Implementation of {@link io.helidon.common.http.Content}.
 */
public final class InBoundContent implements io.helidon.common.http.Content {

    private final Publisher<DataChunk> originalPublisher;
    private final InBoundMediaSupport mediaSupport;

    /**
     * Create a new instance.
     * @param originalPublisher the original publisher for this content
     * @param mediaSupport in-bound media support
     */
    public InBoundContent(Publisher<DataChunk> originalPublisher,
            InBoundMediaSupport mediaSupport) {

        Objects.requireNonNull(originalPublisher, "publisher is null!");
        Objects.requireNonNull(mediaSupport, "mediaSupport is null!");
        this.originalPublisher = originalPublisher;
        this.mediaSupport = mediaSupport;
    }

    /**
     * Copy constructor.
     * @param orig original instance to copy
     */
    public InBoundContent(InBoundContent orig) {
        Objects.requireNonNull(orig, "orig is null!");
        this.originalPublisher = orig.originalPublisher;
        this.mediaSupport = orig.mediaSupport;
    }

    /**
     * Get the in-bound media support.
     * @return InBoundMediaSupport
     */
    public InBoundMediaSupport mediaSupport() {
        return mediaSupport;
    }

    @Override
    public void registerFilter(Filter filter) {
        mediaSupport.registerFilter(filter);
    }

    @Override
    public <T> void registerStreamReader(Class<T> clazz,
            StreamReader<T> reader) {

        mediaSupport.registerStreamReader(clazz, reader);
    }

    @Override
    public <T> void registerReader(Class<T> type, Reader<T> reader) {
        mediaSupport.registerReader(type, reader);
    }

    @Override
    public <T> void registerReader(Predicate<Class<T>> predicate,
            Reader<T> reader) {

        mediaSupport.registerReader(predicate, reader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> as(final Class<T> type) {
        Span readSpan = createReadSpan(type);
        CompletionStage<T> result = mediaSupport
                .unmarshall(type, originalPublisher);
        result.thenRun(readSpan::finish)
                .exceptionally(t -> {
                    finishSpanWithError(readSpan, t);
                    return null;
                });
        return result;
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            Span readSpan = createReadSpan(Publisher.class);
            mediaSupport.applyFilters(originalPublisher)
                    .subscribe(new TracedSubscriber(readSpan, subscriber));
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @Override
    public <T> Publisher<T> asPublisherOf(Class<T> type) {
        return mediaSupport.unmarshallStream(type, originalPublisher);
    }

    @Override
    public <T> Publisher<T> asPublisherOf(GenericType<T> type) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * Create a new span with operation name called "content-read". If a non
     * {@code null} type is provided, a tag named "requested.type" is created
     * with the value being the type name.
     * @param <T> type
     * @param type requested type, may be {@code null}
     * @return created span
     */
    private <T> Span createReadSpan(Class<T> type) {
        Tracer.SpanBuilder spanBuilder = mediaSupport
                .context()
                .createSpanBuilder("content-read");
        if (type != null) {
            spanBuilder.withTag("requested.type", type.getName());
        }
        return spanBuilder.start();
    }

    /**
     * Finish the specified span with the specified error.
     *
     * @param readSpan the span to finish
     * @param ex the error to report
     */
    private static void finishSpanWithError(Span readSpan, Throwable ex) {
        Tags.ERROR.set(readSpan, Boolean.TRUE);
        readSpan.log(CollectionsHelper.mapOf("event", "error",
                "error.kind", "Exception",
                "error.object", ex,
                "message", ex.toString()));
        readSpan.finish();
    }

    /**
     * Delegated subscriber that finishes the specified span during onComplete
     * and onError.
     */
    private static final class TracedSubscriber
            implements Subscriber<DataChunk> {

        private final Span readSpan;
        private final Subscriber<? super DataChunk> delegate;

        TracedSubscriber(Span readSpan,
                Subscriber<? super DataChunk> subscriber) {

            this.readSpan = readSpan;
            this.delegate = subscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(DataChunk item) {
            delegate.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                delegate.onError(throwable);
            } finally {
                finishSpanWithError(readSpan, throwable);
            }
        }

        @Override
        public void onComplete() {
            try {
                delegate.onComplete();
            } finally {
                readSpan.finish();
            }
        }
    }
}
