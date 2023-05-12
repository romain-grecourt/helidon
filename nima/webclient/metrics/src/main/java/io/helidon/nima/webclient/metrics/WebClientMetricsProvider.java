package io.helidon.nima.webclient.metrics;

import io.helidon.common.config.Config;
import io.helidon.nima.webclient.WebClientService;
import io.helidon.nima.webclient.spi.WebClientServiceProvider;

/**
 * Service for web client metrics.
 */
public class WebClientMetricsProvider implements WebClientServiceProvider {

    @Override
    public String configKey() {
        return "metrics";
    }

    @Override
    public WebClientService create(Config config) {
        return WebClientMetrics.create(config);
    }

}
