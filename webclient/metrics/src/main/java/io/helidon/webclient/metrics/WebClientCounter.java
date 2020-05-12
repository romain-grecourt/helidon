/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.webclient.metrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.helidon.common.http.Http;
import io.helidon.webclient.WebClientServiceRequest;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * Client metric counter for all requests.
 */
class WebClientCounter extends WebClientMetric {

    WebClientCounter(Builder builder) {
        super(builder);
    }

    @Override
    MetricType metricType() {
        return MetricType.COUNTER;
    }

    @Override
    public CompletionStage<WebClientServiceRequest> request(WebClientServiceRequest request) {
        Http.RequestMethod method = request.method();

        request.whenResponseReceived()
                .thenAccept(response -> {
                    if (shouldContinueOnError(method, response.status().code())) {
                        updateCounter(createMetadata(request, response));
                    }
                });
        request.whenComplete()
                .thenAccept(response -> {
                    if (shouldContinueOnSuccess(method, response.status().code())) {
                        updateCounter(createMetadata(request, response));
                    }
                })
                .exceptionally(throwable -> {
                    if (shouldContinueOnError(method)) {
                        updateCounter(createMetadata(request, null));
                    }
                    return null;
                });

        return CompletableFuture.completedFuture(request);
    }

    private void updateCounter(Metadata metadata) {
        Counter counter = metricRegistry().counter(metadata);
        counter.inc();
    }

}
