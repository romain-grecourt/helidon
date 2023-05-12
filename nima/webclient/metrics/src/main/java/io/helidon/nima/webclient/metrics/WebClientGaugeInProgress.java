package io.helidon.nima.webclient.metrics;

import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * Gauge which counts all requests in progress.
 */
class WebClientGaugeInProgress extends WebClientMetric {

    WebClientGaugeInProgress(Builder builder) {
        super(builder);
    }

    @Override
    MetricType metricType() {
        return MetricType.CONCURRENT_GAUGE;
    }

    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
        ConcurrentGauge gauge = metricRegistry().concurrentGauge(createMetadata(request, null));
        boolean shouldBeHandled = handlesMethod(request.method());
        try {
            if (shouldBeHandled) {
                gauge.inc();
            }
            WebClientServiceResponse response = chain.proceed(request);
            if (shouldBeHandled) {
                gauge.dec();
            }
            return response;
        } catch (Throwable ex) {
            if (shouldBeHandled) {
                gauge.dec();
            }
            throw ex;
        }
    }

}
