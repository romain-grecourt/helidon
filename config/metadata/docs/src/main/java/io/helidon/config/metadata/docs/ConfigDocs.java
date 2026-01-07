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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.helidon.common.Functions.CheckedSupplier;
import io.helidon.config.metadata.model.ConfigMetadata;
import io.helidon.config.metadata.model.ConfigMetadata.CmAllowedValue;
import io.helidon.config.metadata.model.ConfigMetadata.CmOption;
import io.helidon.config.metadata.model.ConfigMetadata.CmType;

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
class ConfigDocs {
    private static final System.Logger LOGGER = System.getLogger(ConfigDocs.class.getName());

    private final ConfigMetadata metadata;
    private final Map<String, List<Node>> usages;
    private final Path outputDir;
    private final Template typeTemplate;
    private final Template readmeTemplate;

    /**
     * Create a new instance.
     *
     * @param outputDir output directory
     * @param metadata  config metadata
     */
    ConfigDocs(Path outputDir, ConfigMetadata metadata) {
        this.outputDir = outputDir;
        this.metadata = metadata.resolve();
        this.usages = resolveUsages();
        Handlebars handlebars = new Handlebars();
        typeTemplate = template(handlebars, "type.md.hbs");
        readmeTemplate = template(handlebars, "README.md.hbs");
    }

    /**
     * Process the config metadata and generate the corresponding documentation.
     */
    void process() {
        var processedTypes = new TreeSet<String>();
        for (var module : metadata.modules()) {
            LOGGER.log(Level.INFO, "Processing module: {0}", module.module());
            for (var type : module.types()) {
                var fileName = type.type() + ".md";
                generateFile(fileName, () -> typeTemplate.apply(typeContext(type)));
                processedTypes.add(fileName);
                // TODO investigate the duplicates (/tmp/config-refs-dups.txt)
            }
        }

        // generate index
        generateFile("README.md", () -> readmeTemplate.apply(Map.of("types", processedTypes)));

        // remove obsolete files
        try (Stream<Path> stream = Files.list(outputDir)
                .filter(it -> {
                    var fileName = it.getFileName().toString();
                    return !Files.isDirectory(it)
                           && fileName.endsWith(".md")
                           && !fileName.equals("README.md")
                           && !processedTypes.contains(fileName);
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

    private Map<String, List<Node>> resolveUsages() {
        var stack = new ArrayDeque<Node>();
        for (var module : metadata.modules()) {
            for (var type : module.types()) {
                if (type.standalone()) {
                    stack.push(new Node(null, type.prefix().orElseThrow(), type.type()));
                }
            }
        }

        // depth-first traversal from the standalone types
        var usages = new HashMap<String, List<Node>>();
        while (!stack.isEmpty()) {
            var node = stack.pop();
            var options = metadata.type(node.type).map(CmType::options).orElse(List.of());
            for (var i = options.size() - 1; i >= 0; i--) {
                var option = options.get(i);
                var optionType = option.type().orElse(CmOption.DEFAULT_TYPE);
                var child = new Node(node, option.key(), optionType);
                if (option.complex()) {
                    // only record usage of complex types
                    usages.computeIfAbsent(optionType, k -> new ArrayList<>()).add(node);
                }
                stack.push(child);
            }
        }
        return usages;
    }

    private Map<String, Object> typeContext(CmType type) {
        var context = new HashMap<String, Object>();
        var typeName = type.type();
        type.description().ifPresent(it -> context.put("description", it));
        context.put("type", typeName);
        context.put("standalone", type.standalone());
        type.prefix().ifPresent(it -> context.put("prefix", it));
        context.put("provides", type.provides());

        var sortedOptions = new ArrayList<>(type.options());
        sortedOptions.sort(Comparator.comparing(CmOption::key));
        context.put("options", sortedOptions.stream()
                .map(this::optionContext)
                .toList());

        context.put("usages", usages.getOrDefault(typeName, List.of()).stream()
                .map(this::usageContext)
                .toList());
        return context;
    }

    private Map<String, Object> optionContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var tagsContext = new ArrayList<String>();
        if (option.required()) {
            tagsContext.add("required");
        }
        if (option.deprecated()) {
            tagsContext.add("deprecated");
        }
        if (option.experimental()) {
            tagsContext.add("experimental");
        }
        context.put("tags", tagsContext);
        context.put("key", option.key());
        context.put("type", optionTypeContext(option));
        context.put("allowedValues", option.allowedValues().stream()
                .map(this::allowedValueContext)
                .toList());
        option.description().ifPresent(it -> context.put("description", it));
        option.defaultValue().ifPresent(it -> context.put("defaultValue", it));
        context.put("providers", option.providerType().or(option::type)
                .map(metadata::providers)
                .orElseGet(List::of));
        return context;
    }

    private Map<String, Object> optionTypeContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var type = option.type().orElse(CmOption.DEFAULT_TYPE);
        context.put("type", switch (type) {
            case "java.lang.String" -> "string";
            case "java.lang.Integer" -> "int";
            case "java.lang.Boolean" -> "boolean";
            case "java.lang.Long" -> "long";
            case "java.lang.Character" -> "char";
            case "java.lang.Float" -> "float";
            case "java.lang.Double" -> "double";
            default -> type;
        });
        context.put("complex", option.complex());
        switch (option.kind().orElse(CmOption.DEFAULT_KIND)) {
            case VALUE -> context.put("isValue", true);
            case MAP -> context.put("isMap", true);
            case LIST -> context.put("isList", true);
        }
        return context;
    }

    private Map<String, Object> allowedValueContext(CmAllowedValue allowedValue) {
        var context = new HashMap<String, Object>();
        context.put("value", allowedValue.value());
        allowedValue.description().ifPresent(it -> context.put("description", it));
        return context;
    }

    private Map<String, Object> usageContext(Node node) {
        var context = new HashMap<String, Object>();
        context.put("enclosingType", node.enclosingType());
        context.put("key", node.key);
        context.put("path", node.path());
        return context;
    }

    private void generateFile(String name, CheckedSupplier<CharSequence, IOException> content) {
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            LOGGER.log(Level.INFO, "Generating " + name);
            var outputFile = outputDir.resolve(name);
            Files.writeString(outputFile, content.get(), TRUNCATE_EXISTING, CREATE);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate: " + name, ex);
        }
    }

    private static Template template(Handlebars handlebars, String template) {
        URL resource = ConfigDocs.class.getResource(template);
        if (resource == null) {
            throw new IllegalStateException("Failed to locate template: " + template);
        }
        try {
            return handlebars.compile(new URLTemplateSource(template, resource));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template: " + template, e);
        }
    }

    private record Node(Node parent, String key, String type) {

        String enclosingType() {
            return parent != null ? parent.type : null;
        }

        String path() {
            var sb = new StringBuilder(key);
            var node = parent;
            while(node != null) {
                sb.insert(0, node.key + ".");
                node = node.parent;
            }
            return sb.toString();
        }
    }
}
