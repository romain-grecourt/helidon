package io.helidon.nima.webclient.metrics;

import java.util.function.Function;

/**
 * Supported client metric types.
 */
enum WebClientMetricType {

    /**
     * Client counter metric.
     */
    COUNTER(WebClientCounter::new),
    /**
     * Client timer metric.
     */
    TIMER(WebClientTimer::new),
    /**
     * Client gauge in progress metric.
     */
    GAUGE_IN_PROGRESS(WebClientGaugeInProgress::new),
    /**
     * Client meter metric.
     */
    METER(WebClientMeter::new);

    private final Function<WebClientMetric.Builder, WebClientMetric> function;

    WebClientMetricType(Function<WebClientMetric.Builder, WebClientMetric> function) {
        this.function = function;
    }

    WebClientMetric createInstance(WebClientMetric.Builder builder) {
        return function.apply(builder);
    }

}