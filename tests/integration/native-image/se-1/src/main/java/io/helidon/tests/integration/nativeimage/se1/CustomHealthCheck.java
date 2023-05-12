package io.helidon.tests.integration.nativeimage.se1;

import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;

public class CustomHealthCheck implements HealthCheck {

    @Override
    public String name() {
        return "custom";
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.builder()
                                  .status(HealthCheckResponse.Status.UP)
                                  .detail("timestamp", System.currentTimeMillis())
                                  .build();
    }
}
