package io.helidon.openapi.ui;

import io.helidon.common.config.Config;
import io.helidon.openapi.spi.OpenApiServiceProvider;

/**
 * A {@link OpenApiServiceProvider} that provides {@link OpenApiUi}.
 */
public final class OpenApiUiProvider implements OpenApiServiceProvider {

    /**
     * Create a new instance.
     *
     * @deprecated to be used solely by {@link java.util.ServiceLoader}
     */
    @Deprecated
    public OpenApiUiProvider() {
    }

    @Override
    public String configKey() {
        return "ui";
    }

    @Override
    public OpenApiUi create(Config config, String name) {
        return OpenApiUi.builder().config(config).build();
    }
}
