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
import java.util.Optional;
import java.util.concurrent.Flow.Publisher;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.http.ReadOnlyParameters;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.EntitySupport.Reader;
import io.helidon.media.common.EntitySupport.ReaderContext;
import io.helidon.media.common.EntitySupport.StreamReader;

import static io.helidon.media.common.EntitySupport.DEFAULT_CHARSET;

/**
 * Implementation of {@link ReaderContext}.
 */
final class ReaderContextImpl extends AbstractEntityContext<ReaderContextImpl> implements ReaderContext {

    private final Parameters headers;
    private final MediaType contentType;
    private final OperatorRegistry<Reader<?>> readers;
    private final OperatorRegistry<StreamReader<?>> sreaders;

    /**
     * Create a new instance.
     *
     * @param parent        parent context, may be {@code null}
     * @param eventListener event listener, may be {@code null}
     * @param headers       headers, must not be {@code null}
     * @param contentType   content-type, may be {@code null}
     */
    ReaderContextImpl(ReaderContextImpl parent,
                      EventListener eventListener,
                      Parameters headers,
                      MediaType contentType) {

        super(parent, eventListener);
        if (contentType != null) {
            this.contentType = contentType;
        } else if (headers != null) {
            this.contentType = headers().first(Http.Header.CONTENT_TYPE)
                                        .map(MediaType::parse)
                                        .orElse(null);
        } else {
            this.contentType = null;
        }
        this.headers = headers != null ? headers : ReadOnlyParameters.empty();
        if (parent != null) {
            this.readers = new OperatorRegistry<>(parent.readers);
            this.sreaders = new OperatorRegistry<>(parent.sreaders);
        } else {
            this.readers = new OperatorRegistry<>();
            this.sreaders = new OperatorRegistry<>();
        }
    }

    @Override
    public Parameters headers() {
        return headers;
    }

    @Override
    public ReaderContextImpl registerReader(Reader<?> reader) {
        readers.registerFirst(reader);
        return this;
    }

    @Override
    public ReaderContextImpl registerReader(StreamReader<?> reader) {
        sreaders.registerFirst(reader);
        return this;
    }

    @Override
    public <T> Single<T> unmarshall(Publisher<DataChunk> chunks, GenericType<T> type) {
        return unmarshall(chunks, type, read(findReader(type)));
    }

    @Override
    public <U, T extends U> Single<T> unmarshall(Publisher<DataChunk> chunks, Reader<U> reader, GenericType<T> type) {
        return unmarshall(chunks, type, read(reader));
    }

    @Override
    public <T> Multi<T> unmarshallStream(Publisher<DataChunk> chunks, GenericType<T> type) {
        return unmarshall(chunks, type, readStream(findStreamReader(type)));
    }

    @Override
    public <U, T extends U> Multi<T> unmarshallStream(Publisher<DataChunk> chunks,
                                                      StreamReader<U> reader,
                                                      GenericType<T> type) {
        return unmarshall(chunks, type, readStream(reader));
    }

    @Override
    public Optional<MediaType> contentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public Charset charset() throws IllegalStateException {
        if (contentType != null) {
            try {
                return contentType.charset().map(Charset::forName).orElse(DEFAULT_CHARSET);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return DEFAULT_CHARSET;
    }

    @Override
    public ReaderContextImpl createChild(EventListener eventListener, Parameters headers, MediaType contentType) {
        return new ReaderContextImpl(this, eventListener, headers, contentType);
    }

    // Adapter between Reader and StreamReader
    private interface R<U, T extends U, P extends Publisher<T>> {
        P read(Publisher<DataChunk> p, GenericType<T> t);
    }

    private <U, T extends U> R<U, T, Single<T>> read(Reader<U> reader) {
        return (Publisher<DataChunk> p, GenericType<T> t) -> reader.read(p, t, this);
    }

    private <U, T extends U> R<U, T, Multi<T>> readStream(StreamReader<U> reader) {
        return (Publisher<DataChunk> p, GenericType<T> t) -> reader.read(p, t, this);
    }

    @SuppressWarnings("unchecked")
    private <T, U extends T, P extends Publisher<U>> P unmarshall(Publisher<DataChunk> p,
                                                                  GenericType<U> t,
                                                                  R<T, U, P> r) {
        try {
            return (p == null) ? (P) Single.<U>empty() : r.read(applyFilters(p, t), t);
        } catch (Throwable ex) {
            return (P) Single.<T>error(new IllegalStateException("Transformation failed!", ex));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Reader<T> findReader(GenericType<T> type) {
        // Flow.Publisher - can only be supported by streaming media
        if (Publisher.class.isAssignableFrom(type.rawType())) {
            throw new IllegalStateException("This method does not support unmarshalling of Flow.Publisher."
                    + " Please use a stream unmarshalling method.");
        }

        Reader<T> reader = (Reader<T>) readers.select(type, this);
        if (reader == null) {
            throw new IllegalStateException("No reader found for type: " + type);
        }
        return reader;
    }

    @SuppressWarnings("unchecked")
    private <T> StreamReader<T> findStreamReader(GenericType<T> type) {
        StreamReader<T> reader = (StreamReader<T>) sreaders.select(type, this);
        if (reader == null) {
            throw new IllegalStateException("No stream reader found for type: " + type);
        }
        return reader;
    }
}
