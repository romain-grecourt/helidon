package io.helidon.common.http;

import io.helidon.common.GenericType;
import io.helidon.common.reactive.FailedPublisher;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * In-bound reactive payload that can be converted to entities.
 */
@SuppressWarnings("deprecation")
public final class InBoundContent
        implements HttpContent, EntityReadersRegistry, Content {

    private final Publisher<DataChunk> originalPublisher;
    final EntityReaders readers;
    final InBoundContext inBoundContext;

    /**
     * Create a new instance.
     * @param originalPublisher the original publisher for this content
     * @param readers entity readers
     * @param inBoundContext in-bound context
     */
    public InBoundContent(Publisher<DataChunk> originalPublisher,
            EntityReaders readers, InBoundContext inBoundContext) {

        Objects.requireNonNull(originalPublisher, "publisher is null!");
        Objects.requireNonNull(readers, "readers is null!");
        Objects.requireNonNull(inBoundContext, "inBoundContext is null!");
        this.originalPublisher = originalPublisher;
        this.readers = readers;
        this.inBoundContext = inBoundContext;
    }

    /**
     * Copy constructor.
     * @param orig original instance to copy
     */
    public InBoundContent(InBoundContent orig) {
        Objects.requireNonNull(orig, "orig is null!");
        this.originalPublisher = orig.originalPublisher;
        this.readers = orig.readers;
        this.inBoundContext = orig.inBoundContext;
    }

    @Override
    public void registerFilter(ContentFilter filter) {
        readers.registerFilter(filter);
    }

    @Override
    public void registerReader(EntityReader<?> reader) {
        readers.registerReader(reader);
    }

    @Override
    public void registerStreamReader(EntityStreamReader<?> streamReader) {
        readers.registerStreamReader(streamReader);
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        try {
            readers.applyFilters(originalPublisher, inBoundContext)
                    .subscribe(subscriber);
        } catch (Exception e) {
            subscriber.onError(new IllegalArgumentException(
                    "Unexpected exception occurred during publishers chaining",
                    e));
        }
    }

    @Override
    public <T> CompletionStage<T> as(final Class<T> type) {
        return as(type, /* readersDelegate */ null);
    }

    public <T> CompletionStage<T> as(final GenericType<T> type) {
        return as(type, /* readersDelegate */ null);
    }

    public <T> Publisher<T> asStream(Class<T> type) {
        return asStream(type, /* readersDelegate */ null);
    }

    public <T> Publisher<T> asStream(GenericType<T> type) {
        return asStream(type, /* readersDelegate */ null);
    }

    @SuppressWarnings("unchecked")
    <T> CompletionStage<T> as(final Class<T> type,
            EntityReaders readersDelegate) {

        CompletionStage<T> result;
        try {
            ContentInfo contentInfo = inBoundContext.contentInfo();
            EntityReader<T> reader = readers.selectReader(type,
                    contentInfo, readersDelegate);

            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }

            result = (CompletionStage<T>) reader.readEntity(
                    filteredPublisher(type.getTypeName()), type,
                    contentInfo, inBoundContext.defaultCharset());

        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            result = failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            result = failedFuture;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    <T> CompletionStage<T> as(final GenericType<T> type,
            EntityReaders readersDelegate) {

        CompletionStage<T> result;
        try {
            ContentInfo contentInfo = inBoundContext.contentInfo();
            EntityReader<T> reader = readers.selectReader(type,
                    contentInfo, readersDelegate);

            if (reader == null) {
                throw new IllegalArgumentException(
                        "No reader found for class: " + type);
            }

            result = (CompletionStage<T>) reader.readEntity(
                    filteredPublisher(type.getTypeName()), type,
                    contentInfo, inBoundContext.defaultCharset());

        } catch (IllegalArgumentException e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            result = failedFuture;
        } catch (Exception e) {
            CompletableFuture failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new IllegalArgumentException("Transformation failed!", e));
            result = failedFuture;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    <T> Publisher<T> asStream(Class<T> type, EntityReaders readersDelegate) {

        Publisher<T> result;
        try {
            ContentInfo contentInfo = inBoundContext.contentInfo();
            EntityStreamReader<T> streamReader = readers
                    .selectStreamReader(type, contentInfo,
                            readersDelegate);

            if (streamReader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for class: " + type);
            }

            result = (Publisher<T>) streamReader.readEntityStream(
                    filteredPublisher(type.getTypeName()), type,
                    contentInfo, inBoundContext.defaultCharset());

        } catch (IllegalArgumentException e) {
            result = new FailedPublisher<>(e);
        } catch (Exception e) {
            result = new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    <T> Publisher<T> asStream(GenericType<T> type,
            EntityReaders readersDelegate) {

        Publisher<T> result;
        try {
            ContentInfo contentInfo = inBoundContext.contentInfo();
            EntityStreamReader<T> streamReader = readers
                    .selectStreamReader(type, contentInfo,
                            readersDelegate);

            if (streamReader == null) {
                throw new IllegalArgumentException(
                        "No stream reader found for class: " + type);
            }

            result = (Publisher<T>) streamReader.readEntityStream(
                    filteredPublisher(type.getTypeName()), type,
                    contentInfo, inBoundContext.defaultCharset());

        } catch (IllegalArgumentException e) {
            result = new FailedPublisher<>(e);
        } catch (Exception e) {
            result = new FailedPublisher<>(new IllegalArgumentException(
                    "Transformation failed!", e));
        }
        return result;
    }

    private Publisher<DataChunk> filteredPublisher(String type) {
        return readers.applyFilters(originalPublisher,
                new ContentInterceptor.WrappedFactory(inBoundContext, type));
    }
}
