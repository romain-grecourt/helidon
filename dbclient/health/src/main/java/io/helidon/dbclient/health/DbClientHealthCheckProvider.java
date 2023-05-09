package io.helidon.dbclient.health;

import io.helidon.common.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.health.HealthCheck;
import io.helidon.health.spi.HealthCheckProvider;

import java.util.List;

public class DbClientHealthCheckProvider implements HealthCheckProvider {
    @Override
    public List<HealthCheck> healthChecks(Config config) {
        Config dbConfig = config.get("db");
        DbClient dbClient = DbClient.builder(dbConfig).build();
        return List.of(DbClientHealthCheck.create(dbClient, dbConfig.get("health-check")));
    }
}
