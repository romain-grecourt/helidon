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
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.http.EntityWriters;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.helidon.common.http.EntityWriter;
import io.helidon.common.http.EntityStreamWriter;
import io.helidon.common.http.ContentFilter;
import io.helidon.common.http.EntityWritersRegistry;
import io.helidon.common.http.OutBoundScope;
import io.helidon.common.reactive.EmptyPublisher;

/**
 * The basic implementation of {@link ServerResponse}.
 */
abstract class Response implements ServerResponse {

    private final WebServer webServer;
    private final BareResponse bareResponse;
    private final HashResponseHeaders headers;

    private final CompletionStage<ServerResponse> completionStage;
    private final OutBoundScope scope;

    // Content related
    private final SendLockSupport sendLockSupport;

    /**
     * Creates new instance.
     *
     * @param webServer a web server.
     * @param bareResponse an implementation of the response SPI.
     */
    Response(WebServer webServer, BareResponse bareResponse,
            EntityWriters writers) {

        this.webServer = webServer;
        this.bareResponse = bareResponse;
        this.headers = new HashResponseHeaders(bareResponse);
        this.completionStage = bareResponse.whenCompleted().thenApply(a -> this);
        this.sendLockSupport = new SendLockSupport();
        // TODO accepted types
        this.scope = new OutBoundScope(headers, Request.DEFAULT_CHARSET,
                /* acceptedTypes */ null, writers);
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
        this.scope = response.scope;
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
                EntityWriter.Promise<Object> promise = scope.writers
                        .selectWriter(content, scope, null);
                Publisher<DataChunk> publisher = promise.writer
                        .writeEntity(content, promise, scope);
                if (publisher == null) {
                    throw new IllegalArgumentException(
                            "Cannot write! No registered writer for '"
                            + content.getClass().toString() + "'.");
                }
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        scope.writers.applyFilters(publisher));
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
                    ? new EmptyPublisher<>()
                    : content;
            sendLockSupport.execute(() -> {
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        scope.writers.applyFilters(publisher));
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
        return send((Publisher<DataChunk>)null);
    }

    @Override
    public <T> CompletionStage<ServerResponse> send(Publisher<T> content,
            Class<T> itemClass) {

        Span writeSpan = createWriteSpan(content);
        try {
            sendLockSupport.execute(() -> {
                EntityStreamWriter.Promise<T> promise = scope.writers
                        .selectStreamWriter(itemClass, scope, null);
                Publisher<DataChunk> publisher = promise.writer
                        .writeEntityStream(content, itemClass, promise, scope);
                if (publisher == null) {
                    throw new IllegalArgumentException(
                            "Cannot write! No registered stream writer for '"
                            + content.getClass().toString() + "'.");
                }
                if (promise.contentType != null) {
                    headers.put(Http.Header.CONTENT_TYPE,
                            promise.contentType.toString());
                }
                if (promise.contentLength > 0) {
                    headers.put(Http.Header.CONTENT_LENGTH,
                            String.valueOf(promise.contentLength));
                }
                Publisher<DataChunk> pub = new SendHeadersFirstPublisher<>(
                        headers, writeSpan,
                        scope.writers.applyFilters(publisher));
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
            MediaType contentType, EntityStreamWriter<T> writer) {

        // TODO
        return this;
    }

    @Override
    public <T> ServerResponse registerStreamWriter(Predicate<Class<T>> accept,
            MediaType contentType, EntityStreamWriter<T> writer) {

        // TODO
        return this;
    }

    @Override
    public <T> Response registerWriter(Class<T> type, EntityWriter<T> writer) {
        // TODO
        return this;
    }

    @Override
    public <T> Response registerWriter(Class<T> type, MediaType contentType,
            EntityWriter<T> writer) {

        // TODO
        return this;
    }

    @Override
    public <T> Response registerWriter(Predicate<?> accept,
            EntityWriter<T> writer) {

        // TODO
        return this;
    }

    @Override
    public <T> Response registerWriter(Predicate<?> accept,
            MediaType contentType, EntityWriter<T> writer) {

        // TODO
        return this;
    }

    @Override
    public Response registerWriter(EntityWriter<?> writer) {
        scope.writers.registerWriter(writer);
        return this;
    }

    @Override
    public Response registerStreamWriter(EntityStreamWriter<?> writer) {
        scope.writers.registerStreamWriter(writer);
        return this;
    }


    @Override
    public Response registerFilter(ContentFilter filter) {
        scope.writers.registerFilter(filter);
        return this;
    }

    @Override
    public CompletionStage<ServerResponse> whenSent() {
        return completionStage;
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
