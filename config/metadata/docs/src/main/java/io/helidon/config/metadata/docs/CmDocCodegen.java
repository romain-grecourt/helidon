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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.helidon.config.metadata.model.CmModel;
import io.helidon.config.metadata.model.CmModel.CmAllowedValue;
import io.helidon.config.metadata.model.CmModel.CmOption;
import io.helidon.config.metadata.model.CmModel.CmType;
import io.helidon.config.metadata.model.CmNode;
import io.helidon.config.metadata.model.CmResolver;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.URLTemplateSource;

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
        var handlebars = new Handlebars();
        rootTemplate = template(handlebars, "root.md.hbs");
        configTemplate = template(handlebars, "config.md.hbs");
        providerTemplate = template(handlebars, "provider.md.hbs");
        manifestTemplate = template(handlebars, "manifest.md.hbs");
    }

    /**
     * Process the config metadata and generate the corresponding documentation.
     */
    void process() {
        // config types
        var rootTypes = new TreeSet<CmType>();
        var nestedTypes = new TreeSet<CmType>();

        // type names
        var configTypeNames = new TreeSet<String>();
        var providerTypeNames = new TreeSet<String>();

        // traverse the roots
        for (var root : resolver.roots()) {

            // render the root type
            var rootType = root.type().orElseThrow(() -> new IllegalStateException("Root type is unresolved"));
            var rootTypeName = rootType.type();
            generateFile(rootTypeName + ".md", configTemplate, configContext(rootType));
            configTypeNames.add(rootTypeName);
            rootTypes.add(rootType);

            // gather all nested types
            for (var child : root.children()) {
                child.visit(n -> {
                    n.type().ifPresent(nestedTypes::add);
                    return true;
                });
            }
        }

        // generate README.md
        generateFile("README.md", rootTemplate, rootContext(rootTypes));

        // document all nested types
        for (var type : nestedTypes) {
            var typeName = type.type();
            generateFile(typeName + ".md", configTemplate, configContext(type));
            configTypeNames.add(typeName);
        }

        // document all provider contracts
        for (var typeName : resolver.contracts()) {
            generateFile(typeName + ".md", providerTemplate, providerContext(typeName));
            providerTypeNames.add(typeName);
        }

        // generate listing
        generateFile("manifest.md", manifestTemplate, manifestContext(configTypeNames, providerTypeNames));

        // remove obsolete files
        try (Stream<Path> stream = Files.list(outputDir)
                .filter(it -> {
                    var fileName = it.getFileName().toString();
                    if (!Files.isDirectory(it)
                        && fileName.endsWith(".md")
                        && !fileName.equals("README.md")
                        && !fileName.equals("manifest.md")) {

                        var typeName = fileName.substring(0, fileName.length() - 3);
                        return !configTypeNames.contains(typeName) && !providerTypeNames.contains(typeName);
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
        context.put("description", type.description().orElse("-"));
        context.put("type", typeName);

        var options = new TreeSet<>(type.options());
        context.put("options", options.stream()
                .map(this::optionContext)
                .toList());

        context.put("usages", resolver.usage(typeName).stream()
                .map(this::usageContext)
                .toList());

        context.put("allowedValues", options.stream()
                .anyMatch(it -> !it.allowedValues().isEmpty()));
        return context;
    }

    private Map<String, Object> optionContext(CmOption option) {
        var context = new HashMap<String, Object>();
        context.put("key", option.key().orElseThrow());
        context.put("flags", optionFlagsContext(option));
        context.put("type", optionTypeContext(option));
        context.put("description", option.description().orElse("-"));
        option.defaultValue().ifPresent(it -> context.put("defaultValue", it));

        if (!option.allowedValues().isEmpty()) {
            // other cell need to merge
            context.put("rowspan", option.allowedValues().size());

            // allowedValues[0] is rendered on the first row
            option.allowedValues().stream()
                    .limit(1)
                    .findFirst()
                    .ifPresent(it -> context.put("allowedValue1", allowedValueContext(it)));

            // allowedValues[i > 0] are rendered in separate rows
            context.put("allowedValueX", option.allowedValues().stream()
                    .skip(1)
                    .map(this::allowedValueContext)
                    .toList());
        }
        return context;
    }

    private List<String> optionFlagsContext(CmOption option) {
        var context = new ArrayList<String>();
        if (option.required() || option.defaultValue().isEmpty()) {
            context.add("required");
        } else {
            context.add("optional");
        }
        if (option.deprecated()) {
            context.add("deprecated");
        }
        if (option.experimental()) {
            context.add("experimental");
        }
        if (option.provider()) {
            context.add("provider");
        }
        return context;
    }

    private Map<String, Object> optionTypeContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var optionType = resolver.type(option.type());
        var optionTypeName = optionType.map(CmType::type)
                .orElse(option.type());
        if (option.provider() || optionType.isPresent()) {
            context.put("fileName", optionTypeName + ".md");
        }
        context.put("shortName", shortType(optionTypeName));
        context.put("fullName", optionTypeName);
        context.put("kind", option.kind().name());
        return context;
    }

    private Map<String, Object> typeContext(CmType type) {
        var context = new HashMap<String, Object>();
        var prefix = type.prefix()
                .orElseThrow(() -> new IllegalStateException(
                        "Type does not have a prefix: " + type.type()));
        var typeName = type.type();
        context.put("prefix", prefix);
        context.put("fileName", typeName + ".md");
        context.put("shortName", shortType(typeName));
        context.put("description", type.description().orElse("-"));
        return context;
    }

    private Map<String, Object> allowedValueContext(CmAllowedValue allowedValue) {
        var context = new HashMap<String, Object>();
        context.put("value", allowedValue.value());
        context.put("description", allowedValue.description().orElse("-"));
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
                .map(this::typeContext)
                .toList());
        context.put("usages", resolver.usage(typeName).stream()
                .map(this::usageContext)
                .toList());
        return context;
    }

    private Map<String, Object> rootContext(Set<CmType> roots) {
        var context = new HashMap<String, Object>();
        context.put("roots", roots.stream()
                .map(this::typeContext)
                .toList());
        return context;
    }

    private Map<String, Object> manifestContext(Set<String> configTypes, Set<String> providerTypes) {
        var context = new HashMap<String, Object>();
        context.put("configTypes", configTypes);
        context.put("providerTypes", providerTypes);
        return context;
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
        URL resource = CmDocCodegen.class.getResource(template);
        if (resource == null) {
            throw new IllegalStateException("Failed to locate template: " + template);
        }
        try {
            return handlebars.compile(new URLTemplateSource(template, resource));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template: " + template, e);
        }
    }

    private static String shortType(String typeName) {
        var index = typeName.lastIndexOf('.');
        if (index >= 0) {
            return typeName.substring(index + 1);
        }
        return typeName;
    }
}
