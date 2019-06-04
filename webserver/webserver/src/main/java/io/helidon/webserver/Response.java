/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Filter;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.StreamWriter;
import io.helidon.common.http.Writer;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.ReactiveStreamsAdapter;
import io.helidon.webserver.internal.OutBoundContext;
import io.helidon.webserver.internal.OutBoundMediaSupport;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import reactor.core.publisher.Mono;

/**
 * The basic implementation of {@link ServerResponse}.
 */
// HACK - made public expose Writer for now
public abstract class Response implements ServerResponse {

    private final WebServer webServer;
    private final BareResponse bareResponse;
    private final HashResponseHeaders headers;

    private final CompletionStage<ServerResponse> completionStage;
    private final OutBoundMediaSupport mediaSupport;

    // Content related
    private final SendLockSupport sendLockSupport;

    /**
     * Creates new instance.
     *
     * @param webServer a web server.
     * @param bareResponse an implementation of the response SPI.
     */
    Response(WebServer webServer, BareResponse bareResponse) {
        this.webServer = webServer;
        this.bareResponse = bareResponse;
        this.headers = new HashResponseHeaders(bareResponse);
        this.completionStage = bareResponse.whenCompleted().thenApply(a -> this);
        this.sendLockSupport = new SendLockSupport();
        this.mediaSupport = new OutBoundMediaSupport(new OutBoundContext() {

            @Override
            public void setContentType(MediaType mediaType) {
                headers.contentType(mediaType);
            }

            @Override
            public void setContentLength(long size) {
                headers.contentLength(size);
            }
        });
    }

    /**
     * Creates clone of existing instance.
     *
     * @param response a response to clone.
     */
    Response(Response response) {
        this.webServer = response.webServer;
        this.bareResponse = response.bareResponse;
        this.headers = response.headers;
        this.completionStage = response.completionStage;
        this.sendLockSupport = response.sendLockSupport;
        this.mediaSupport = response.mediaSupport;
    }

    /**
     * Returns a span context related to the current request.
     * <p>
     * {@code SpanContext} is a tracing component from
     * <a href="http://opentracing.io">opentracing.io</a> standard.
     * </p>
     *
     * @return the related span context
     */
    abstract SpanContext spanContext();

    @Override
    public WebServer webServer() {
        return webServer;
    }

    @Override
    public Http.ResponseStatus status() {
        return headers.httpStatus();
    }

    @Override
    public Response status(Http.ResponseStatus status) {
        Objects.requireNonNull(status, "Parameter 'status' was null!");
        headers.httpStatus(status);
        return this;
    }

    @Override
    public ResponseHeaders headers() {
        return headers;
    }

    private Tracer tracer() {
        Tracer result = null;
        if (webServer != null) {
            ServerConfiguration configuration = webServer.configuration();
            if (configuration != null) {
                result = configuration.tracer();
            }
        }
        return result == null ? GlobalTracer.get() : result;
    }

    private <T> Span createWriteSpan(T obj) {
        Tracer.SpanBuilder spanBuilder = tracer().buildSpan("content-write");
        if (spanContext() != null) {
            spanBuilder.asChildOf(spanContext());
        }
        if (obj != null) {
            spanBuilder.withTag("response.type", obj.getClass().getName());
        }
        return spanBuilder.start();
    }

    @Override
    public <T> CompletionStage<ServerResponse> send(T content) {
        Span writeSpan = createWriteSpan(content);
        try {
            sendLockSupport.execute(() -> {
                Publisher<DataChunk> publisher = mediaSupport.marshall(content,
                        headers.contentType().orElse(null));
                if (publisher == null) {
                    throw new IllegalArgumentException(
                            "Cannot write! No registered writer for '"
                            + content.getClass().toString() + "'.");
                }
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        mediaSupport.applyFilters(publisher));
                sendLockSupport.contentSend = true;
                pub.subscribe(bareResponse);
            }, content == null);
            return whenSent();
        } catch (RuntimeException | Error e) {
            writeSpan.finish();
            throw e;
        }
    }

    @Override
    public CompletionStage<ServerResponse> send(Publisher<DataChunk> content) {
        Span writeSpan = createWriteSpan(content);
        try {
            Publisher<DataChunk> publisher = (content == null)
                    ? ReactiveStreamsAdapter.publisherToFlow(Mono.empty())
                    : content;
            sendLockSupport.execute(() -> {
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        mediaSupport.applyFilters(publisher));
                sendLockSupport.contentSend = true;
                pub.subscribe(bareResponse);
            }, content == null);
            return whenSent();
        } catch (RuntimeException | Error e) {
            writeSpan.finish();
            throw e;
        }
    }

    @Override
    public CompletionStage<ServerResponse> send() {
        return send(null);
    }

    @Override
    public <T> CompletionStage<ServerResponse> send(Publisher<T> content,
            Class<T> itemClass) {

        Span writeSpan = createWriteSpan(content);
        try {
            sendLockSupport.execute(() -> {
                Publisher<DataChunk> publisher = mediaSupport
                        .marshallStream(content, itemClass, headers
                                .contentType().orElse(null));
                if (publisher == null) {
                    throw new IllegalArgumentException(
                            "Cannot write! No registered stream writer for '"
                            + content.getClass().toString() + "'.");
                }
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        mediaSupport.applyFilters(publisher));
                sendLockSupport.contentSend = true;
                pub.subscribe(bareResponse);
            }, content == null);
            return whenSent();
        } catch (RuntimeException | Error e) {
            writeSpan.finish();
            throw e;
        }
    }

    @Override
    public <T> ServerResponse registerStreamWriter(Class<T> acceptType,
            MediaType contentType, StreamWriter<T> writer) {

        mediaSupport.registerStreamWriter(acceptType, contentType, writer);
        return this;
    }

    @Override
    public <T> ServerResponse registerStreamWriter(Predicate<Class<T>> accept,
            MediaType contentType, StreamWriter<T> writer) {

        mediaSupport.registerStreamWriter(accept, contentType, writer);
        return this;
    }

    @Override
    public <T> Response registerWriter(Class<T> type, Writer<T> writer) {
        mediaSupport.registerWriter(type, /* contentType */ null, writer);
        return this;
    }

    @Override
    public <T> Response registerWriter(Class<T> type, MediaType contentType,
            Writer<T> writer) {

        mediaSupport.registerWriter(type, contentType, writer);
        return this;
    }

    @Override
    public <T> Response registerWriter(Predicate<?> accept,
            Writer<T> writer) {

        mediaSupport.registerWriter(accept, /* contentType */ null, writer);
        return this;
    }

    @Override
    public <T> Response registerWriter(Predicate<?> accept,
            MediaType contentType, Writer<T> writer) {

        mediaSupport.registerWriter(accept, contentType,
                writer);
        return this;
    }

    @Override
    public Response registerFilter(Filter filter) {
        mediaSupport.registerFilter(filter);
        return this;
    }

    @Override
    public CompletionStage<ServerResponse> whenSent() {
        return completionStage;
    }

    public OutBoundMediaSupport mediaSupport() {
        return mediaSupport;
    }

    @Override
    public long requestId() {
        return bareResponse.requestId();
    }

    private static class SendLockSupport {

        private boolean contentSend = false;

        private synchronized void execute(Runnable runnable,
                boolean silentSendStatus) {

            // test effective close
            if (contentSend) {
                if (silentSendStatus) {
                    return;
                } else {
                    throw new IllegalStateException(
                            "Response is already sent!");
                }
            }
            runnable.run();
        }
    }
}
