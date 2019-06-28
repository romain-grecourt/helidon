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

import io.helidon.common.GenericType;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.helidon.common.reactive.EmptyPublisher;
import java.util.List;
import io.helidon.common.http.MessageBody;
import io.helidon.common.http.MessageBodyContextBase;
import io.helidon.common.http.MessageBodyWriterContext;
import io.helidon.common.reactive.SingleItemPublisher;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The basic implementation of {@link ServerResponse}.
 */
abstract class Response implements ServerResponse {

    private final WebServer webServer;
    private final BareResponse bareResponse;
    private final HashResponseHeaders headers;

    private final CompletionStage<ServerResponse> completionStage;
    private final MessageBodyWriterContext writerContext;
    private final MessageBodyEventListener eventListener;

    // Content related
    private final SendLockSupport sendLockSupport;

    /**
     * Creates new instance.
     *
     * @param webServer a web server.
     * @param bareResponse an implementation of the response SPI.
     */
    Response(WebServer webServer, BareResponse bareResponse,
            List<MediaType> acceptedTypes) {

        this.webServer = webServer;
        this.bareResponse = bareResponse;
        this.headers = new HashResponseHeaders(bareResponse);
        this.completionStage = bareResponse.whenCompleted().thenApply(a -> this);
        this.sendLockSupport = new SendLockSupport();
        this.eventListener = new MessageBodyEventListener();
        this.writerContext = MessageBodyWriterContext.create(
                webServer.mediaSupport().writerContext(), eventListener,
                headers, acceptedTypes);
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
        this.writerContext = response.writerContext;
        this.eventListener = response.eventListener;
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
                Publisher<DataChunk> sendPublisher = writerContext
                        .marshall(new SingleItemPublisher<>(content),
                                GenericType.create((Class<T>)content.getClass()));
                sendLockSupport.contentSend = true;
                sendPublisher.subscribe(bareResponse);
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
            Publisher<DataChunk> sendPublisher = (content == null)
                    ? new EmptyPublisher<>()
                    : writerContext.applyFilters(content);
            sendLockSupport.execute(() -> {
                sendLockSupport.contentSend = true;
                sendPublisher.subscribe(bareResponse);
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
                GenericType<T> type = GenericType.create(itemClass);
                Publisher<DataChunk> sendPublisher = writerContext
                        .marshallStream(content, type);
                sendLockSupport.contentSend = true;
                sendPublisher.subscribe(bareResponse);
            }, content == null);
            return whenSent();
        } catch (RuntimeException | Error e) {
            writeSpan.finish();
            throw e;
        }
    }

    @Override
    public Response registerWriter(MessageBody.Writer<?> writer) {
        writerContext.registerWriter(writer);
        return this;
    }

    @Override
    public Response registerStreamWriter(MessageBody.Writer<?> writer) {
        writerContext.registerStreamWriter(writer);
        return this;
    }

    @Override
    public Response registerFilter(MessageBody.Filter filter) {
        writerContext.registerFilter(filter);
        return this;
    }

    @Deprecated
    @Override
    public void registerFilter(
            Function<Publisher<DataChunk>, Publisher<DataChunk>> function) {

        writerContext.registerFilter(function);
    }

    @Deprecated
    @Override
    public <T> MessageBody.Writers registerWriter(Class<T> type,
            Function<T, Publisher<DataChunk>> function) {

        writerContext.registerWriter(type, function);
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBody.Writers registerWriter(Predicate<?> accept,
            Function<T, Publisher<DataChunk>> function) {

        writerContext.registerWriter(accept, function);
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBody.Writers registerWriter(Class<T> type,
            MediaType contentType,
            Function<? extends T, Publisher<DataChunk>> function) {

        writerContext.registerWriter(type, contentType, function);
        return this;
    }

    @Deprecated
    @Override
    public <T> MessageBody.Writers registerWriter(Predicate<?> accept,
            MediaType contentType, Function<T, Publisher<DataChunk>> function) {

        writerContext.registerWriter(accept, contentType, function);
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

    private final class MessageBodyEventListener
            implements MessageBodyContextBase.EventListener {

        private Span span;

        // Sent switch just once from false to true near the beginning.
        // It use combination with volatile to faster check.
        private boolean sent;
        private volatile boolean sentVolatile;

        @Override
        public void onEvent(MessageBodyContextBase.Event event) {
            switch(event.eventType()) {
                case BEFORE_ONSUBSCRIBE:
                    Tracer.SpanBuilder spanBuilder = tracer()
                            .buildSpan("content-write");
                    if (spanContext() != null) {
                        spanBuilder.asChildOf(spanContext());
                    }
                    GenericType<?> type = event.entityType().orElse(null);
                    if (type != null) {
                        spanBuilder.withTag("response.type", type.getTypeName());
                    }
                    span = spanBuilder.start();
                    break;
                case BEFORE_ONNEXT:
                    // send headers if needed
                    if (headers != null && !sent && !sentVolatile) {
                        synchronized (this) {
                            if (!sent && !sentVolatile) {
                                sent = true;
                                sentVolatile = true;
                                headers.send();
                            }
                        }
                    }
                    break;
                case AFTER_ONERROR:
                    if (span != null) {
                        span.finish();
                    }
                    break;
                case BEFORE_ONCOMPLETE:
                case AFTER_ONCOMPLETE:
                    if (span != null) {
                        // no-op if called more than once
                        span.finish();
                    }
                    break;
                default:
                    // do nothing
            }
        }
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
