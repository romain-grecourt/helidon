package io.helidon.nima.webclient.metrics;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;

import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Timer;

import java.time.Duration;

/**
 * Timer which measures the length of request.
 */
class WebClientTimer extends WebClientMetric {

    WebClientTimer(WebClientMetric.Builder builder) {
        super(builder);
    }

    @Override
    MetricType metricType() {
        return MetricType.TIMER;
    }

    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
        long start = System.nanoTime();
        Http.Method method = request.method();
        try {
            WebClientServiceResponse response = chain.proceed(request);
            Http.Status status = response.status();
            if (shouldContinueOnError(method, status.code())) {
                updateTimer(createMetadata(request, response), start);
            }
            return response;
        } catch (Throwable ex) {
            if (shouldContinueOnError(method)) {
                updateTimer(createMetadata(request, null), start);
            }
            throw ex;
        }
    }

    private void updateTimer(Metadata metadata, long start) {
        long time = System.nanoTime() - start;
        Timer timer = metricRegistry().timer(metadata);
        timer.update(Duration.ofNanos(time));
    }

}