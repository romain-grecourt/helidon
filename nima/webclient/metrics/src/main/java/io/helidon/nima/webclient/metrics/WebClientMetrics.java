package io.helidon.nima.webclient.metrics;

import io.helidon.common.config.Config;
import io.helidon.nima.webclient.WebClientService;
import io.helidon.nima.webclient.WebClientServiceRequest;
import io.helidon.nima.webclient.WebClientServiceResponse;

import java.util.ArrayList;
import java.util.List;

public class WebClientMetrics implements WebClientService {

    private final List<WebClientMetric> metrics;

    private WebClientMetrics(Builder builder) {
        metrics = builder.metrics;
    }

    /**
     * Creates new timer client metric.
     *
     * @return client metric builder
     */
    public static WebClientMetric.Builder timer() {
        return WebClientMetric.builder(WebClientMetricType.TIMER);
    }

    /**
     * Creates new counter client metric.
     *
     * @return client metric builder
     */
    public static WebClientMetric.Builder counter() {
        return WebClientMetric.builder(WebClientMetricType.COUNTER);
    }

    /**
     * Creates new meter client metric.
     *
     * @return client metric builder
     */
    public static WebClientMetric.Builder meter() {
        return WebClientMetric.builder(WebClientMetricType.METER);
    }

    /**
     * Creates new gauge in progress client metric.
     *
     * @return client metric builder
     */
    public static WebClientMetric.Builder gaugeInProgress() {
        return WebClientMetric.builder(WebClientMetricType.GAUGE_IN_PROGRESS);
    }

    /**
     * Creates new client metrics based on config.
     *
     * @param config config
     * @return client metrics instance
     */
    public static WebClientMetrics create(Config config) {
        WebClientMetrics.Builder builder = new Builder();
        config.asNodeList().ifPresent(configs ->
                configs.forEach(metricConfig ->
                        builder.register(processClientMetric(metricConfig))));
        return builder.build();
    }

    private static WebClientMetric processClientMetric(Config metricConfig) {
        String type = metricConfig.get("type").asString().orElse("COUNTER");
        return switch (type) {
            case "COUNTER" -> counter().config(metricConfig).build();
            case "METER" -> meter().config(metricConfig).build();
            case "TIMER" -> timer().config(metricConfig).build();
            case "GAUGE_IN_PROGRESS" -> gaugeInProgress().config(metricConfig).build();
            default -> throw new IllegalStateException(String.format(
                    "Metrics type %s is not supported through service loader",
                    type));
        };
    }

    @Override
    public WebClientServiceResponse handle(Chain chain, WebClientServiceRequest request) {
        return chain.proceed(request);
    }

    private static final class Builder implements io.helidon.common.Builder<Builder, WebClientMetrics> {

        private final List<WebClientMetric> metrics = new ArrayList<>();

        private Builder() {
        }

        private Builder register(WebClientMetric clientMetric) {
            metrics.add(clientMetric);
            return this;
        }

        @Override
        public WebClientMetrics build() {
            return new WebClientMetrics(this);
        }
    }
}
