/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.media.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.helidon.common.Builder;
import io.helidon.common.serviceloader.HelidonServiceLoader;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.media.common.MediaContext.Readers;
import io.helidon.media.common.MediaContext.Writers;
import io.helidon.media.common.spi.MediaSupportProvider;

/**
 * {@link MediaContext} builder.
 */
public final class MediaContextBuilder implements Builder<MediaContextBuilder, MediaContext>,
                                                  MediaContext.Builder<MediaContextBuilder> {

    private static final String SERVICE_NAME = "name";
    private static final String DEFAULTS_NAME = "defaults";

    private static final int DEFAULTS_PRIORITY = 100;
    private static final int BUILDER_PRIORITY = 200;
    private static final int LOADER_PRIORITY = 300;

    private final HelidonServiceLoader.Builder<MediaSupportProvider> services = HelidonServiceLoader
            .builder(ServiceLoader.load(MediaSupportProvider.class));

    private final List<MediaSupport.Reader<?>> builderReaders = new ArrayList<>();
    private final List<MediaSupport.StreamReader<?>> builderStreamReaders = new ArrayList<>();
    private final List<MediaSupport.Writer<?>> builderWriters = new ArrayList<>();
    private final List<MediaSupport.StreamWriter<?>> builderStreamWriter = new ArrayList<>();
    private final List<MediaSupport> mediaSupports = new ArrayList<>();
    private final Map<String, Map<String, String>> servicesConfig = new HashMap<>();
    private final MediaContext.ReaderContext readerContext;
    private final MediaContext.WriterContext writerContext;
    private boolean registerDefaults = true;
    private boolean discoverServices = false;
    private boolean filterServices = false;

    MediaContextBuilder() {
        this.readerContext = MediaContext.ReaderContext.create();
        this.writerContext = MediaContext.WriterContext.create();
    }

    /**
     * Configures this {@link MediaContextBuilder} from the supplied {@link Config}.
     * <table class="config">
     * <caption>Optional configuration parameters</caption>
     * <tr>
     *     <th>key</th>
     *     <th>description</th>
     * </tr>
     * <tr>
     *     <td>register-defaults</td>
     *     <td>Whether to register default reader and writers</td>
     * </tr>
     * <tr>
     *     <td>discover-services</td>
     *     <td>Whether to discover services via service loader</td>
     * </tr>
     * <tr>
     *     <td>filter-services</td>
     *     <td>Whether to filter discovered services by service names in services section</td>
     * </tr>
     * <tr>
     *     <td>services</td>
     *     <td>Configuration section for each service. Each entry has to have "name" parameter.
     *     It is also used for filtering of loaded services.</td>
     * </tr>
     * </table>
     *
     * @param config a {@link Config}
     * @return this {@link MediaContextBuilder}
     */
    public MediaContextBuilder config(Config config) {
        config.get("register-defaults").asBoolean().ifPresent(this::registerDefaults);
        config.get("discover-services").asBoolean().ifPresent(this::discoverServices);
        config.get("filter-services").asBoolean().ifPresent(this::filterServices);
        config.get("services")
              .asNodeList()
              .ifPresent(it -> it.forEach(serviceConfig -> {
                  String name = serviceConfig.get(SERVICE_NAME).asString().get();
                  servicesConfig.merge(name,
                          serviceConfig.detach().asMap().orElseGet(Map::of),
                          (first, second) -> {
                              HashMap<String, String> result = new HashMap<>(first);
                              result.putAll(second);
                              return result;
                          });
              }));
        return this;
    }

    @Override
    public MediaContextBuilder addMediaSupport(MediaSupport mediaSupport) {
        Objects.requireNonNull(mediaSupport);
        mediaSupports.add(mediaSupport);
        return this;
    }

    /**
     * Adds new instance of {@link MediaSupport} with specific priority.
     *
     * @param mediaSupport media support
     * @param priority     priority
     * @return updated instance of the builder
     */
    public MediaContextBuilder addMediaSupport(MediaSupport mediaSupport, int priority) {
        Objects.requireNonNull(mediaSupport);
        services.addService((config) -> mediaSupport, priority);
        return this;
    }

    @Override
    public MediaContextBuilder addReader(MediaSupport.Reader<?> reader) {
        builderReaders.add(reader);
        return this;
    }

    @Override
    public MediaContextBuilder addStreamReader(MediaSupport.StreamReader<?> streamReader) {
        builderStreamReaders.add(streamReader);
        return this;
    }

    @Override
    public MediaContextBuilder addWriter(MediaSupport.Writer<?> writer) {
        builderWriters.add(writer);
        return this;
    }

    @Override
    public MediaContextBuilder addStreamWriter(MediaSupport.StreamWriter<?> streamWriter) {
        builderStreamWriter.add(streamWriter);
        return this;
    }

    /**
     * Whether defaults should be included.
     *
     * @param registerDefaults register defaults
     * @return this builder instance
     */
    public MediaContextBuilder registerDefaults(boolean registerDefaults) {
        this.registerDefaults = registerDefaults;
        return this;
    }

    /**
     * Whether Java Service Loader should be used to load {@link MediaSupportProvider}.
     *
     * @param discoverServices use Java Service Loader
     * @return this builder instance
     */
    public MediaContextBuilder discoverServices(boolean discoverServices) {
        this.discoverServices = discoverServices;
        return this;
    }

    /**
     * Whether services loaded by Java Service Loader should be filtered.
     * All of the services which should pass the filter, have to be present under {@code services} section of configuration.
     *
     * @param filterServices filter services
     * @return this builder instance
     */
    public MediaContextBuilder filterServices(boolean filterServices) {
        this.filterServices = filterServices;
        return this;
    }

    @Override
    public MediaContext build() {
        //Remove all service names from the obtained service configurations
        servicesConfig.forEach((key, values) -> values.remove(SERVICE_NAME));
        if (filterServices) {
            this.services.useSystemServiceLoader(false);
            filterServices();
        } else {
            this.services.useSystemServiceLoader(discoverServices);
        }
        if (registerDefaults) {
            this.services.addService(new DefaultsProvider(), DEFAULTS_PRIORITY);
        }
        this.services.defaultPriority(LOADER_PRIORITY)
                     .addService(config -> new MediaSupport() {
                         @Override
                         public void register(Readers readersRegistry, Writers writersRegistry) {
                             builderReaders.forEach(readersRegistry::registerReader);
                             builderStreamReaders.forEach(readersRegistry::registerReader);
                             builderWriters.forEach(writersRegistry::registerWriter);
                             builderStreamWriter.forEach(writersRegistry::registerWriter);
                         }
                     }, BUILDER_PRIORITY)
                     .addService(config -> new MediaSupport() {
                         @Override
                         public void register(Readers readersRegistry, Writers writersRegistry) {
                             mediaSupports.forEach(it -> it.register(readersRegistry, writersRegistry));
                         }
                     }, BUILDER_PRIORITY)
                     .build()
                     .asList()
                     .stream()
                     .map(it -> it.create(Config.just(ConfigSources.create(servicesConfig.getOrDefault(it.configKey(),
                             new HashMap<>())))))
                     .collect(Collectors.toCollection(LinkedList::new))
                     .descendingIterator()
                     .forEachRemaining(mediaService -> mediaService.register(readerContext, writerContext));

        return new MediaContextImpl(readerContext, writerContext);
    }

    private void filterServices() {
        HelidonServiceLoader.builder(ServiceLoader.load(MediaSupportProvider.class))
                            .defaultPriority(LOADER_PRIORITY)
                            .build()
                            .asList()
                            .stream()
                            .filter(provider -> servicesConfig.containsKey(provider.configKey()))
                            .forEach(services::addService);
    }

    private static final class DefaultsProvider implements MediaSupportProvider {

        @Override
        public String configKey() {
            return MediaContextBuilder.DEFAULTS_NAME;
        }

        @Override
        public MediaSupport create(Config config) {
            return DefaultMediaSupport.builder()
                                      .config(config)
                                      .build();
        }
    }
}
