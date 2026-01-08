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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.helidon.common.Functions.CheckedSupplier;
import io.helidon.config.metadata.model.CmModel;
import io.helidon.config.metadata.model.CmModel.CmAllowedValue;
import io.helidon.config.metadata.model.CmNode;
import io.helidon.config.metadata.model.CmModel.CmOption;
import io.helidon.config.metadata.model.CmResolver;
import io.helidon.config.metadata.model.CmModel.CmType;

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
    private final Template typeTemplate;
    private final Template readmeTemplate;

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
        typeTemplate = template(handlebars, "type.md.hbs");
        readmeTemplate = template(handlebars, "README.md.hbs");
    }

    // TODO investigate the duplicates (/tmp/config-refs-dups.txt)

    /**
     * Process the config metadata and generate the corresponding documentation.
     */
    void process() {
        var processedTypes = new TreeSet<String>();

        // gather all types reachable from the true roots
        var types = new HashSet<CmType>();
        for (var root : resolver.tree()) {
            root.visit(n -> {
                n.type().ifPresent(types::add);
                return true;
            });
        }

        // document all reachable types
        for (var type : types) {
            var fileName = type.type() + ".md";
            generateFile(fileName, () -> typeTemplate.apply(typeContext(type)));
            processedTypes.add(fileName);
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

        context.put("usages", resolver.usage(typeName).stream()
                .filter(it -> it.parent().isPresent())
                .sorted(Comparator.comparing(CmNode::key))
                .map(this::usageContext)
                .toList());
        return context;
    }

    private Map<String, Object> optionContext(CmOption option) {
        var context = new HashMap<String, Object>();
        context.put("key", option.key());
        context.put("flags", optionFlagsContext(option));
        context.put("type", optionTypeContext(option));
        context.put("rowspan", Math.max(option.allowedValues().size(), 1));
        option.allowedValues().stream()
                .limit(1)
                .findFirst()
                .ifPresent(it -> context.put("firstAllowedValue", allowedValueContext(it)));
        context.put("otherAllowedValues", option.allowedValues().stream()
                .skip(1)
                .map(this::allowedValueContext)
                .toList());
        option.description().ifPresent(it -> context.put("description", it));
        option.defaultValue().ifPresent(it -> context.put("defaultValue", it));
        context.put("providers", option.providerType().or(option::type)
                .map(resolver::providers)
                .orElseGet(List::of));
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
        return context;
    }

    private Map<String, Object> optionTypeContext(CmOption option) {
        var context = new HashMap<String, Object>();
        var resolvedType = option.type().flatMap(resolver::type);
        var configTypeName = resolvedType.map(CmType::type).orElse(CmOption.DEFAULT_TYPE);
        context.put("resolved", resolvedType.isPresent());
        context.put("shortName", shortType(configTypeName));
        context.put("fullName", configTypeName);
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

    private Map<String, Object> usageContext(CmNode node) {
        var context = new HashMap<String, Object>();
        context.put("enclosingType", node.parent().map(CmNode::typeName).orElseThrow());
        context.put("key", node.key());
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
