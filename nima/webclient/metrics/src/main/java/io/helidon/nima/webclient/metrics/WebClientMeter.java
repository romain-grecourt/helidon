package io.helidon.nima.webclient.metrics;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricType;

/**
 * Client metric meter for all requests.
 */
public class WebClientMeter extends WebClientMetric {

    WebClientMeter(Builder builder) {
        super(builder);
    }

    @Override
    MetricType metricType() {
        return MetricType.METERED;
    }

    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
        Http.Method method = request.method();
        try {
            WebClientServiceResponse response = chain.proceed(request);
            Http.Status status = response.status();
            if (shouldContinueOnError(method, status.code())) {
                updateMeter(createMetadata(request, null));
            }
            return response;
        } catch (Throwable ex) {
            if (shouldContinueOnError(method)) {
                updateMeter(createMetadata(request, null));
            }
            throw ex;
        }
    }

    private void updateMeter(Metadata metadata) {
        Meter meter = metricRegistry().meter(metadata);
        meter.mark();
    }
}
