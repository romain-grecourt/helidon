/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.media.common;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Predicate;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.HashParameters;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.StreamWriter;
import io.helidon.media.common.EntitySupport.Writer;
import io.helidon.media.common.EntitySupport.WriterContext;

import static io.helidon.media.common.EntitySupport.DEFAULT_CHARSET;

/**
 * Implementation of {@link WriterContext}.
 */
final class WriterContextImpl extends AbstractEntityContext<WriterContextImpl> implements WriterContext {

    private final Parameters headers;
    private final List<MediaType> acceptedTypes;
    private final OperatorRegistry<Writer<?>> writers;
    private final OperatorRegistry<StreamWriter<?>> swriters;
    private boolean contentType;
    private MediaType contentTypeSet;
    private boolean charsetCached;
    private Charset charsetCache;

    /**
     * Create a new instance.
     *
     * @param parent        parent context, may be {@code null}
     * @param eventListener event listener, may be {@code null}
     * @param headers       headers, must not be {@code null}
     * @param acceptedTypes accepted types, may be {@code null}
     */
    WriterContextImpl(WriterContextImpl parent,
                      EventListener eventListener,
                      Parameters headers,
                      List<MediaType> acceptedTypes) {

        super(parent, eventListener);
        this.headers = headers != null ? headers : HashParameters.create();
        this.acceptedTypes = Objects.requireNonNullElseGet(acceptedTypes, List::of);
        if (parent != null) {
            this.writers = new OperatorRegistry<>(parent.writers);
            this.swriters = new OperatorRegistry<>(parent.swriters);
            this.contentTypeSet = parent.contentTypeSet;
            this.contentType = parent.contentType;
            this.charsetCache = parent.charsetCache;
            this.charsetCached = parent.charsetCached;
        } else {
            this.writers = new OperatorRegistry<>();
            this.swriters = new OperatorRegistry<>();
        }
    }

    @Override
    public Parameters headers() {
        return headers;
    }

    @Override
    public WriterContext createChild(EventListener eventListener, Parameters headers, List<MediaType> acceptedTypes) {
        return new WriterContextImpl(this, eventListener, headers, acceptedTypes);
    }

    @Override
    public WriterContext registerWriter(Writer<?> writer) {
        writers.registerFirst(writer);
        return this;
    }

    @Override
    public WriterContext registerWriter(StreamWriter<?> writer) {
        swriters.registerFirst(writer);
        return this;
    }

    @Override
    public <T> Publisher<DataChunk> marshall(Single<T> content, GenericType<T> type) {
        return marshall(content, type, write(findWriter(type)));
    }

    @Override
    public <U, T extends U> Publisher<DataChunk> marshall(Single<T> content, Writer<U> writer, GenericType<T> type) {
        return marshall(content, type, write(writer));
    }

    @Override
    public <T> Publisher<DataChunk> marshallStream(Publisher<T> content, GenericType<T> type) {
        return marshall(content, type, writeStream(findStreamWriter(type)));
    }

    @Override
    public <U, T extends U> Publisher<DataChunk> marshallStream(Publisher<T> content,
                                                                StreamWriter<U> writer,
                                                                GenericType<T> type) {
        return marshall(content, type, writeStream(writer));
    }

    @Override
    public Optional<MediaType> contentType() {
        if (!contentType) {
            contentTypeSet = headers().first(Http.Header.CONTENT_TYPE).map(MediaType::parse).orElse(null);
            contentType = true;
        }
        return Optional.ofNullable(contentTypeSet);
    }

    @Override
    public void contentType(MediaType contentType) {
        if (contentType != null) {
            headers().putIfAbsent(Http.Header.CONTENT_TYPE, contentType.toString());
        }
    }

    @Override
    public void contentLength(long contentLength) {
        if (contentLength >= 0) {
            headers().putIfAbsent(Http.Header.CONTENT_LENGTH, String.valueOf(contentLength));
        }
    }

    @Override
    public List<MediaType> acceptedTypes() {
        return acceptedTypes;
    }

    @Override
    public MediaType findAccepted(Predicate<MediaType> predicate, MediaType defaultType) throws IllegalStateException {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        Objects.requireNonNull(defaultType, "defaultType cannot be null");
        MediaType contentType = contentType().orElse(null);
        if (contentType == null) {
            if (acceptedTypes.isEmpty()) {
                return defaultType;
            } else {
                for (final MediaType acceptedType : acceptedTypes) {
                    if (predicate.test(acceptedType)) {
                        if (acceptedType.isWildcardType() || acceptedType.isWildcardSubtype()) {
                            return defaultType;
                        }
                        return MediaType.create(acceptedType.type(), acceptedType.subtype());
                    }
                }
            }
        } else {
            if (predicate.test(contentType)) {
                return contentType;
            }
        }
        throw new IllegalStateException("No accepted Content-Type");
    }

    @Override
    public MediaType findAccepted(MediaType mediaType) throws IllegalStateException {
        Objects.requireNonNull(mediaType, "mediaType cannot be null");
        for (MediaType acceptedType : acceptedTypes) {
            if (mediaType.equals(acceptedType)) {
                return acceptedType;
            }
        }
        throw new IllegalStateException("No accepted Content-Type");
    }

    @Override
    public Charset charset() throws IllegalStateException {
        if (charsetCached) {
            return charsetCache;
        }
        MediaType contentType = contentType().orElse(null);
        if (contentType != null) {
            try {
                charsetCache = contentType.charset().map(Charset::forName).orElse(DEFAULT_CHARSET);
                charsetCached = true;
                return charsetCache;
            } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        charsetCache = DEFAULT_CHARSET;
        charsetCached = true;
        return charsetCache;
    }

    // Adapter between Writer and StreamWriter
    private interface W<T, U extends T, P extends Publisher<U>> {
        Publisher<DataChunk> write(P u, GenericType<U> t);
    }

    private <T, U extends T> W<T, U, Single<U>> write(Writer<T> writer) {
        return (Single<U> c, GenericType<U> t) -> writer.write(c, t, this);
    }

    private <T, U extends T> W<T, U, Publisher<U>> writeStream(StreamWriter<T> writer) {
        return (Publisher<U> c, GenericType<U> t) -> writer.write(c, t, this);
    }

    private <T, U extends T, P extends Publisher<U>> Publisher<DataChunk> marshall(P c, GenericType<U> t, W<T, U, P> w) {
        try {
            return applyFilters(c != null ? w.write(c, t) : Multi.empty());
        } catch (Throwable ex) {
            throw new IllegalStateException("Transformation failed!", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Writer<T> findWriter(GenericType<T> type) {
        // Flow.Publisher - can only be supported by streaming media
        if (Publisher.class.isAssignableFrom(type.rawType())) {
            throw new IllegalStateException("This method does not support marshalling of Flow.Publisher."
                    + " Please use a method that accepts Flow.Publisher and type for stream marshalling.");
        }

        Writer<T> writer = (Writer<T>) writers.select(type, this);
        if (writer == null) {
            throw new IllegalStateException("No writer found for type: " + type
                    + ". This usually occurs when the appropriate MediaSupport has not been added.");
        }
        return writer;
    }

    @SuppressWarnings("unchecked")
    private <T> StreamWriter<T> findStreamWriter(GenericType<T> type) {
        StreamWriter<T> writer = (StreamWriter<T>) swriters.select(type, this);
        if (writer == null) {
            throw new IllegalStateException("No stream writer found for type: " + type
                    + ". This usually occurs when the appropriate MediaSupport has not been added.");
        }
        return writer;
    }
}
