/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.openapi;

import java.util.Map;
import java.util.function.Function;

import io.helidon.builder.api.Prototype;
import io.helidon.common.media.type.MediaType;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

/**
 * {@link OpenApiUi} prototype.
 */
@Prototype.Blueprint
@Configured
interface OpenApiUiConfigBlueprint {
    /**
     * Merges implementation-specific UI options.
     *
     * @return options for the UI to merge
     */
    @ConfiguredOption(kind = ConfiguredOption.Kind.MAP)
    Map<String, String> options();

    /**
     * Sets whether the UI should be enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @ConfiguredOption(key = "enabled", value = "true")
    boolean isEnabled();

    /**
     * Full web context (not just the suffix) for the UI.
     *
     * @return full web context path for the UI
     */
    @ConfiguredOption
    String webContext();

    /**
     * Assigns how the OpenAPI UI can obtain a formatted document for a given media type.
     * <p>
     *     Developers typically do not invoke this method. Helidon invokes it internally.
     * </p>
     *
     * @return function for obtaining the formatted document
     */
    Function<MediaType, String> documentPreparer();

    /**
     * Assigns the web context the {@code OpenAPISupport} instance uses.
     * <p>
     *     Developers typically do not invoke this method. Helidon invokes it internally.
     * </p>
     * @return the web context used by the {@code OpenAPISupport} service
     */
    String openApiSupportWebContext();
}
