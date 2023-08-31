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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.BadRequestException;
import io.helidon.http.Http;
import io.helidon.http.HttpMediaType;
import io.helidon.openapi.spi.OpenApiUiProvider;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Helidon Support for OpenAPI.
 */
@RuntimeType.PrototypedBy(OpenApiFeatureConfig.class)
public class OpenApiFeature implements HttpFeature, RuntimeType.Api<OpenApiFeatureConfig> {

    private static final System.Logger LOGGER = System.getLogger(OpenApiFeature.class.getName());

    /**
     * Returns a new builder for preparing an {@code OpenApiFeature}.
     *
     * @return new builder
     */
    public static OpenApiFeatureConfig.Builder builder() {
        return OpenApiFeatureConfig.builder();
    }

    /**
     * Create a new support with default configuration.
     *
     * @return a new OpenAPI support
     */
    public static OpenApiFeature create() {
        return builder().build();
    }

    /**
     * Create a new web server from its configuration.
     *
     * @param featureConfig configuration
     * @return a new OpenAPI support
     */
    static OpenApiFeature create(OpenApiFeatureConfig featureConfig) {
        return new OpenApiFeature(featureConfig);
    }

    /**
     * Create a new support with custom configuration.
     *
     * @param builderConsumer consumer of configuration builder
     * @return a new OpenAPI support
     */
    public static OpenApiFeature create(Consumer<OpenApiFeatureConfig.Builder> builderConsumer) {
        OpenApiFeatureConfig.Builder b = OpenApiFeatureConfig.builder();
        builderConsumer.accept(b);
        return b.build();
    }

    private static final DumperOptions JSON_DUMPER_OPTIONS = jsonDumperOptions();
    private static final DumperOptions YAML_DUMPER_OPTIONS = yamlDumperOptions();
    private static final String DEFAULT_STATIC_FILE_PATH_PREFIX = "META-INF/openapi.";
    private static final Map<String, MediaType> SUPPORTED_FORMATS = Map.of(
            "json", MediaTypes.APPLICATION_JSON,
            "yaml", MediaTypes.APPLICATION_OPENAPI_YAML,
            "yml", MediaTypes.APPLICATION_OPENAPI_YAML);
    private static final List<String> DEFAULT_FILE_PATHS = SUPPORTED_FORMATS.keySet()
            .stream()
            .map(fileType -> DEFAULT_STATIC_FILE_PATH_PREFIX + fileType)
            .toList();
    private static final List<MediaType> PREFERRED_MEDIA_TYPES = List.of(
            MediaTypes.APPLICATION_OPENAPI_YAML,
            MediaTypes.APPLICATION_X_YAML,
            MediaTypes.APPLICATION_YAML,
            MediaTypes.APPLICATION_OPENAPI_JSON,
            MediaTypes.APPLICATION_JSON,
            MediaTypes.TEXT_X_YAML,
            MediaTypes.TEXT_YAML);

    private final String content;
    private final List<MediaType> uiMediaTypes;
    private final MediaType[] preferredMediaTypes;
    private final OpenApiFeatureConfig config;
    private final BiConsumer<ServerRequest, ServerResponse> uiHandler;
    private final ConcurrentMap<OpenApiMediaType, String> cachedDocuments = new ConcurrentHashMap<>();

    protected OpenApiFeature(OpenApiFeatureConfig config) {
        this.config = config;
        String staticFile = builder().staticFile();
        if (staticFile != null) {
            content = readContent(staticFile);
            if (content == null) {
                LOGGER.log(Level.WARNING, "Static OpenAPI file not found: %s", staticFile);
            }
        } else {
            String defaultContent = null;
            for (String path : DEFAULT_FILE_PATHS) {
                defaultContent = readContent(path);
                if (defaultContent != null) {
                    break;
                }
            }
            content = defaultContent;
            if (content == null) {
                LOGGER.log(Level.WARNING, "Static OpenAPI file not found, checked: %s", DEFAULT_FILE_PATHS);
            }
        }
        List<MediaType> uiMediaTypes = null;
        BiConsumer<ServerRequest, ServerResponse> uiHandler = null;
        if (config.ui().isEnabled()) {
            OpenApiUiProvider provider = loadUiProvider();
            if (provider != null) {
                uiMediaTypes = provider.supportedMediaTypes();
                uiHandler = provider.handler();
            }
        }
        this.uiMediaTypes = uiMediaTypes != null ? uiMediaTypes : List.of();
        this.uiHandler = uiHandler != null ? uiHandler : (req, res) -> res.next();
        this.preferredMediaTypes = Stream.of(PREFERRED_MEDIA_TYPES, this.uiMediaTypes)
                .flatMap(Collection::stream)
                .toArray(MediaType[]::new);
    }

    @Override
    public OpenApiFeatureConfig prototype() {
        return config;
    }

    @Override
    public void setup(HttpRouting.Builder routing) {
        routing.get("/", this::handle);
    }

    private String formatContent(MediaType mediaType) {
        OpenApiMediaType openApiMediaType = OpenApiMediaType.byMediaType(mediaType);
        if (openApiMediaType == OpenApiMediaType.UNSUPPORTED) {
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "Requested media type %s not supported", mediaType.toString());
            }
        }
        return cachedDocuments.computeIfAbsent(openApiMediaType, this::doFormatContent);
    }

    /**
     * Returns the OpenAPI document content in {@code String} form given the requested media type.
     *
     * @param mediaType media type to use for formatting
     * @return {@code String} containing the formatted OpenAPI document
     */
    protected String doFormatContent(OpenApiMediaType mediaType) {
        return switch (mediaType) {
            case UNSUPPORTED, YAML -> toYaml(content);
            case JSON -> toJson(content);
        };
    }

    private String toYaml(String rawData) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Converting OpenAPI document in YAML format");
        }
        Yaml yaml = new Yaml(YAML_DUMPER_OPTIONS);
        Object loadedData = yaml.load(rawData);
        return yaml.dump(loadedData);
    }

    private String toJson(String data) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Converting OpenAPI document in JSON format");
        }
        Yaml yaml = new Yaml(JSON_DUMPER_OPTIONS);
        Object loadedData = yaml.load(data);
        return yaml.dump(loadedData);
    }

    private void handle(ServerRequest req, ServerResponse res) {
        String format = req.query().first("format").map(String::toLowerCase).orElse(null);
        if (format != null) {
            MediaType contentType = SUPPORTED_FORMATS.get(format.toLowerCase());
            if (contentType == null) {
                throw new BadRequestException(String.format(
                        "Unsupported format: %s, supported formats: %s",
                        format, SUPPORTED_FORMATS.keySet()));
            }
            res.status(Http.Status.OK_200);
            res.headers().contentType(contentType);
            res.send(formatContent(contentType));
        } else {
            HttpMediaType contentType = req.headers()
                    .bestAccepted(preferredMediaTypes)
                    .map(HttpMediaType::create)
                    .orElse(null);

            if (contentType == null) {
                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE, "Accepted types not supported: %s", req.headers().acceptedTypes());
                }
                res.status(Http.Status.UNSUPPORTED_MEDIA_TYPE_415);
                res.send();
                return;
            }

            if (config.ui().isEnabled() && uiMediaTypes.stream().anyMatch(contentType::test)) {
                uiHandler.accept(req, res);
                return;
            }

            res.status(Http.Status.OK_200);
            res.headers().contentType(contentType);
            res.send(formatContent(contentType));
        }
    }

    private static OpenApiUiProvider loadUiProvider() {
        return HelidonServiceLoader.create(ServiceLoader.load(OpenApiUiProvider.class))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static DumperOptions yamlDumperOptions() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(2);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return dumperOptions;
    }

    private static DumperOptions jsonDumperOptions() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        dumperOptions.setSplitLines(false);
        return dumperOptions;
    }

    private static String readContent(String path) {
        try {
            Path file = Path.of(path);
            if (Files.exists(file)) {
                return Files.readString(file);
            } else {
                try(InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                    return is != null ? new String(is.readAllBytes()) : null;
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
