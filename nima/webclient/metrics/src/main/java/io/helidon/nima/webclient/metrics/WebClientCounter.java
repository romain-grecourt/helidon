package io.helidon.nima.webclient.metrics;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;
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
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
        Http.Method method = request.method();
        try {
            WebClientServiceResponse response = chain.proceed(request);
            Http.Status status = response.status();
            if (shouldContinueOnError(method, status.code())) {
                updateCounter(createMetadata(request, response));
            }
            return response;
        } catch (Throwable ex) {
            if (shouldContinueOnError(method)) {
                updateCounter(createMetadata(request, null));
            }
            throw ex;
        }
    }

    private void updateCounter(Metadata metadata) {
        Counter counter = metricRegistry().counter(metadata);
        counter.inc();
    }

}
