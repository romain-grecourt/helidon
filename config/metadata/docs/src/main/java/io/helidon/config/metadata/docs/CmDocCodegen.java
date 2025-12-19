/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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

package io.helidon.config.metadata.docs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.helidon.config.metadata.model.CmModel;
import io.helidon.config.metadata.model.CmModel.CmAllowedValue;
import io.helidon.config.metadata.model.CmModel.CmEnum;
import io.helidon.config.metadata.model.CmModel.CmOption;
import io.helidon.config.metadata.model.CmModel.CmType;
import io.helidon.config.metadata.model.CmNode;
import io.helidon.config.metadata.model.CmResolver;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import static io.helidon.config.metadata.docs.CmDocNames.shortTypeName;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Config docs generator.
 *
 * @see #process()
 */
class CmDocCodegen {
    private static final System.Logger LOGGER = System.getLogger(CmDocCodegen.class.getName());

    private final CmResolver resolver;
    private final Path outputDir;
    private final Template rootTemplate;
    private final Template configTemplate;
    private final Template enumTemplate;
    private final Template providerTemplate;
    private final Template manifestTemplate;

    /**
     * Create a new instance.
     *
     * @param outputDir output directory
     * @param metadata  config metadata
     */
    CmDocCodegen(Path outputDir, CmModel metadata) {
        this.outputDir = outputDir;
        this.resolver = CmResolver.create(metadata);
        var loader = new ClassPathTemplateLoader("/io/helidon/config/metadata/docs");
        var handlebars = new Handlebars(loader);
        rootTemplate = template(handlebars, "root.md");
        configTemplate = template(handlebars, "config.md");
        enumTemplate = template(handlebars, "enum.md");
        providerTemplate = template(handlebars, "provider.md");
        manifestTemplate = template(handlebars, "manifest.md");
    }

    /**
     * Process the config metadata and generate the corresponding documentation.
     */
    void process() {

        // generate README.md
        generateFile("README.md", rootTemplate, rootContext());

        var typeNames = new HashSet<String>();

        // document all types
        for (var type : resolver.types()) {
            var typeName = type.type();
            generateFile(typeName + ".md", configTemplate, configContext(type));
            typeNames.add(typeName);
        }

        // document all provider contracts
        for (var typeName : resolver.contracts()) {
            generateFile(typeName + ".md", providerTemplate, providerContext(typeName));
            typeNames.add(typeName);
        }

        // document enum types
        for (var type : resolver.enums()) {
            var typeName = type.type();
            generateFile(typeName + ".md", enumTemplate, enumContext(type));
            typeNames.add(typeName);
        }

        // generate listing
        generateFile("manifest.md", manifestTemplate, manifestContext());

        // remove obsolete files
        try (Stream<Path> stream = Files.list(outputDir)
                .filter(it -> {
                    var fileName = it.getFileName().toString();
                    if (!Files.isDirectory(it)
                        && fileName.endsWith(".md")
                        && !fileName.equals("README.md")
                        && !fileName.equals("manifest.md")) {

                        var typeName = fileName.substring(0, fileName.length() - 3);
                        return !typeNames.contains(typeName);
                    }
                    return false;
                })) {

            var toRemove = stream.map(Path::toAbsolutePath).toList();
            for (var file : toRemove) {
                LOGGER.log(Level.INFO, "Removing obsolete file: {0}", file);
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> configContext(CmType type) {
        var context = new HashMap<String, Object>();
        var typeName = type.type();
        context.put("description", typeDescription(type));
        context.put("type", typeName);

        context.put("usages", resolver.usage(typeName).stream()
                .map(this::usageContext)
                .toList());

        context.put("options", optionsContext(type.options().stream()
                .filter(o -> !o.deprecated() && !o.experimental())
                .toList()));

        context.put("experimentalOptions", optionsContext(type.options().stream()
                .filter(o -> !o.deprecated() && o.experimental())
                .toList()));

        context.put("deprecatedOptions", optionsContext(type.options().stream()
                .filter(CmOption::deprecated)
                .toList()));
        return context;
    }

    private Map<String, Object> enumContext(CmEnum type) {
        var context = new HashMap<String, Object>();
        var typeName = type.type();
        context.put("type", typeName);
        context.put("usages", resolver.usage(typeName).stream()
                .map(this::usageContext)
                .toList());
        context.put("values", type.values().stream()
                .map(this::allowedValueContext)
                .toList());
        return context;
    }

    private Map<String, Object> optionsContext(List<CmOption> options) {
        var context = new HashMap<String, Object>();
        context.put("options", options.stream().sorted()
                .map(this::optionContext)
                .toList());
        context.put("defaultValues", options.stream()
                .anyMatch(it -> it.defaultValue().isPresent()));
        return context;
    }

    private Map<String, Object> optionContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var key = option.key().orElseThrow();
        var description = option.description().orElse(null);
        if (description == null || description.isBlank()) {
            LOGGER.log(Level.WARNING, "Option does not have a description: {0}", key);
            description = "<code>N/A</code>";
        }
        context.put("key", key);
        context.put("type", optionTypeContext(option));
        context.put("description", description);
        option.defaultValue().ifPresent(it -> context.put("defaultValue", it));
        return context;
    }

    private Map<String, Object> optionTypeContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var optionType = resolver.type(option.type());
        var optionTypeName = optionType.map(CmType::type).orElse(option.type());
        if (option.provider() || optionType.isPresent() || resolver.isEnum(optionTypeName)) {
            context.put("fileName", optionTypeName + ".md");
        }
        context.put("shortName", shortTypeName(optionTypeName));
        context.put("fullName", optionTypeName);
        context.put("kind", option.kind().name());
        return context;
    }

    private Map<String, Object> typeContext(CmType type) {
        var context = new HashMap<String, Object>();
        var typeName = type.type();
        context.put("prefix", type.prefix()
                .orElseThrow(() -> new IllegalStateException(
                        "Type does not have a prefix: " + typeName)));
        context.put("fileName", typeName + ".md");
        context.put("shortName", shortTypeName(typeName));
        context.put("description", typeDescription(type));
        return context;
    }

    private Map<String, Object> allowedValueContext(CmAllowedValue allowedValue) {
        var context = new HashMap<String, Object>();
        context.put("value", allowedValue.value());
        context.put("description", allowedValue.description());
        return context;
    }

    private Map<String, Object> usageContext(CmNode node) {
        var context = new HashMap<String, Object>();
        var fileName = node.parent()
                .map(it -> it.typeName() + ".md")
                .orElse("README.md");
        context.put("fileName", fileName);
        context.put("key", node.key());
        context.put("path", node.path());
        return context;
    }

    private Map<String, Object> providerContext(String typeName) {
        var context = new HashMap<String, Object>();
        context.put("type", typeName);
        context.put("implementations", resolver.providers(typeName).stream()
                .sorted()
                .map(this::typeContext)
                .toList());
        context.put("usages", resolver.usage(typeName).stream()
                .map(this::usageContext)
                .toList());
        return context;
    }

    private Map<String, Object> rootContext() {
        var context = new HashMap<String, Object>();
        context.put("roots", resolver.roots().stream()
                .flatMap(it -> it.type().stream())
                .sorted(Comparator.comparing((CmType it) -> it.prefix().orElseThrow())
                        .thenComparing(CmType::type))
                .map(this::typeContext)
                .toList());
        return context;
    }

    private Map<String, Object> manifestContext() {
        var context = new HashMap<String, Object>();
        context.put("configTypes", resolver.types().stream()
                .map(CmType::type)
                .toList());
        context.put("providerTypes", resolver.contracts());
        context.put("enumTypes", resolver.enums().stream()
                .map(CmEnum::type)
                .toList());
        return context;
    }

    private String typeDescription(CmType type) {
        var description = type.description().orElse(null);
        if (description == null || description.isBlank()) {
            LOGGER.log(Level.WARNING, "Type does not have a description: {0}", type.type());
            description = "<code>N/A</code>";
        }
        return description;
    }

    private void generateFile(String name, Template template, Map<String, Object> context) {
        try {
            LOGGER.log(Level.INFO, "Generating " + name);
            var outputFile = outputDir.resolve(name);
            var outputFileParent = outputFile.getParent();
            if (outputFileParent != null) {
                Files.createDirectories(outputFileParent);
            }
            try (var writer = Files.newBufferedWriter(outputFile, TRUNCATE_EXISTING, CREATE)) {
                template.apply(context, writer);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate: " + name, ex);
        }
    }

    private static Template template(Handlebars handlebars, String template) {
        try {
            return handlebars.compile(template);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template: " + template, e);
        }
    }
}
