package io.helidon.tests.integration.nativeimage.se1;

import io.helidon.common.config.Config;
import io.helidon.health.HealthCheck;
import io.helidon.health.spi.HealthCheckProvider;

import java.util.List;

public class CustomHealthCheckProvider implements HealthCheckProvider {
    @Override
    public List<HealthCheck> healthChecks(Config config) {
        return List.of(new CustomHealthCheck());
    }
}
