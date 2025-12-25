/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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
import java.util.Set;
import java.util.stream.Stream;

import io.helidon.config.metadata.docs.ConfigMetadata.CmModule;
import io.helidon.config.metadata.docs.ConfigMetadata.CmOption;
import io.helidon.config.metadata.docs.ConfigMetadata.CmType;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.URLTemplateSource;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Entry point to generate Helidon config documentation.
 *
 * @see #process()
 */
class ConfigDocs {
    private static final System.Logger LOGGER = System.getLogger(ConfigDocs.class.getName());
    private static final Map<String, String> TYPE_MAPPING;

    static {
        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("java.lang.String", "string");
        typeMapping.put("java.lang.Integer", "int");
        typeMapping.put("java.lang.Boolean", "boolean");
        typeMapping.put("java.lang.Long", "long");
        typeMapping.put("java.lang.Character", "char");
        typeMapping.put("java.lang.Float", "float");
        typeMapping.put("java.lang.Double", "double");
        TYPE_MAPPING = Map.copyOf(typeMapping);
    }

    private final Map<String, CmType> configTypes = new HashMap<>();
    private final Map<CmType, CmModule> modulesByTypes = new HashMap<>();
    private final Set<String> allTypes = new HashSet<>();
    private final List<ConfigMetadata> metadatas;
    private final Set<String> generatedFiles = new HashSet<>();
    private final Path outputDir;
    private final Template typeTemplate;
    private final Template readmeTemplate;

    /**
     * Create a new instance.
     *
     * @param outputDir output directory, must {@code REAMDE.md} or be empty
     * @throws IllegalArgumentException if the output directory is not valid
     */
    ConfigDocs(Path outputDir, List<ConfigMetadata> metadatas) {
        validateOutputDir(outputDir);
        this.outputDir = outputDir;
        this.metadatas = metadatas;
        for (var cm : metadatas) {
            for (var module : cm.modules()) {
                for (var type : module.types()) {
                    configTypes.put(type.annotatedType(), type);
                    allTypes.add(type.annotatedType());
                    allTypes.add(type.type());
                    modulesByTypes.put(type, module);
                }
            }
        }

        // compile templates
        Handlebars handlebars = new Handlebars();
        typeTemplate = template(handlebars, "type.md.hbs");
        readmeTemplate = template(handlebars, "README.md.hbs");
    }

    /**
     * Process the {@code META-INF/helidon/config-metadata.json} files from all dependencies of Helidon, and generate
     * config reference documentation for them.
     * <p>
     * The documentation is updated, including copyright years
     */
    public void process() {
        // add all inherited options to each type
        resolveTypes();

        // add all options from merged types as direct options to each type
        resolveMerges();

        // generate config docs
        for (var metadata : metadatas) {
            for (var module : metadata.modules()) {
                LOGGER.log(Level.INFO, "Documenting module " + module.module());
                for (var type : module.types()) {
                    generateConfigDoc(type);
                }
            }
        }

        // generate README.md
        generateReadme();

        // and now report obsolete files
        // filter out generated files
        try (Stream<Path> stream = Files.list(outputDir)
                .filter(it -> it.getFileName().toString().endsWith(".md"))
                .filter(it -> !it.getFileName().toString().equals("README.md"))
                .filter(it -> !generatedFiles.contains(String.valueOf(it.getFileName())))) {

            var obsoleteFiles = stream.map(Path::toAbsolutePath).toList();
            for (var file : obsoleteFiles) {
                LOGGER.log(Level.WARNING, "File {0} should be deleted, as its config metadata no longer exists", file);
            }
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to discover obsolete files in " + outputDir.toAbsolutePath(), e);
        }
    }

    private void generateConfigDoc(CmType type) {
        try {
            var sortedOptions = new ArrayList<>(type.options());
            sortedOptions.sort(Comparator.comparing(CmOption::key));

            var requiredOptions = new ArrayList<Map<String, Object>>();
            for (var option : sortedOptions) {
                if (option.required()) {
                    requiredOptions.add(Map.of(
                            "key", option.key(),
                            "deprecated", option.deprecated(),
                            "refType", refType(option),
                            "providers", optionProviders(option),
                            "allowedValues", option.allowedValues(),
                            "defaultValue", option.defaultValue(),
                            "description", option.description()));
                }
            }

            var optionalOptions = new ArrayList<Map<String, Object>>();
            for (var option : sortedOptions) {
                if (!option.required()) {
                    optionalOptions.add(Map.of(
                            "key", option.key(),
                            "deprecated", option.deprecated(),
                            "refType", refType(option),
                            "providers", optionProviders(option),
                            "allowedValues", option.allowedValues(),
                            "defaultValue", option.defaultValue(),
                            "description", option.description()));
                }
            }

            var context = new HashMap<String, Object>();
            var typeName = type.type();
            context.put("title", titleFromTypeName(typeName));
            context.put("description", type.description());
            context.put("type", typeName);
            if (typeName.startsWith("io.helidon")) {
                var module = modulesByTypes.get(type);
                if (module == null) {
                    throw new IllegalStateException("Module not found for type: " + typeName);
                }
                var moduleName = module.module();
                var javadocType = toJavadocLink(typeName);
                var javadocUrl = "https://helidon.io/docs/latest/apidocs/%s/%s)".formatted(moduleName, javadocType);
                context.put("javadocUrl", javadocUrl); // TODO update template to use it
            }
            context.put("standalone", type.standalone());
            context.put("prefix", type.prefix());
            context.put("provides", type.provides());
            context.put("requiredOptions", requiredOptions);
            context.put("optionalOptions", optionalOptions);

            var content = typeTemplate.apply(context);

            generateFile(fileNameFromTypeName(type.type()), content);

            // generate a file for the annotated type to avoid conflicts
            if (!type.annotatedType().startsWith(type.type())) {
                generateFile(fileNameFromTypeName(type.annotatedType()), content);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.ERROR, "Failed to render content for type: " + type.type(), ex);
        }
    }

    private void generateReadme() {
        var readmeFile = outputDir.resolve("README.md");
        LOGGER.log(Level.INFO, "Generating " + readmeFile);

        try {
            // sort alphabetically by page title
            var sortedFiles = new ArrayList<>(generatedFiles);
            sortedFiles.sort(Comparator.comparing(ConfigDocs::titleFromFileName));

            var data = new ArrayList<Map<String, String>>();
            for (var file : sortedFiles) {
                var title = titleFromFileName(file);
                data.add(Map.of("file", file, "title", title));
            }
            var content = readmeTemplate.apply(Map.of("data", data));

            Files.writeString(readmeFile,
                    content,
                    TRUNCATE_EXISTING,
                    CREATE);
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Failed to generate readme " + readmeFile, e);
        }
    }

    private boolean isDocumented(String typeName) {
        if (allTypes.contains(typeName)) {
            return true;
        }
        var path = fileNameFromTypeName(typeName);
        return Files.exists(outputDir.resolve(path));
    }

    private String refType(CmOption option) {
        var mapped = TYPE_MAPPING.get(option.type());
        if (mapped != null) {
            return mapped;
        }
        if (!option.provider()) {
            var displayType = displayType(option.kind(), option.type());
            return toLink(option.type(), displayType);
        } else {
            return byKind(option.kind(), option.type());
        }
    }

    private List<Map<String, ?>> optionProviders(CmOption option) {
        var providers = new ArrayList<Map<String, ?>>();
        var type = option.type();
        var providerType = option.providerType();
        for (var e : findProviders(providerType == null ? type : providerType)) {
            providers.add(Map.of(
                    "prefix", e.prefix(),
                    "path", e.type(), // TODO relative path of the .md file
                    "name", e.shortType()));
        }
        return providers;
    }

    // TODO handle this in the template
    private String toLink(String type, String displayType) {
        if (type.startsWith("io.helidon")) {
            if (type.equals("io.helidon.config.Config") || type.equals("io.helidon.common.config.Config")) {
                return "Map&lt;string, string&gt; (documented for specific cases)";
            }
            if (isDocumented(type)) {
                return "[%s](%s.md)".formatted(displayType, type.replace('.', '_'));
            }
        }
        return displayType;
    }

    private List<CmType> findProviders(String typeName) {
        return configTypes.values()
                .stream()
                .filter(it -> !it.provides().isEmpty())
                .filter(it -> it.provides().contains(typeName))
                .toList();
    }

    private static String displayType(CmOption.Kind kind, String type) {
        int lastIndex = type.lastIndexOf('.');
        if (lastIndex == -1) {
            return byKind(kind, type);
        }
        String name = type.substring(lastIndex + 1);
        if ("Builder".equals(name)) {
            String base = type.substring(0, lastIndex);
            lastIndex = base.lastIndexOf('.');
            if (lastIndex == -1) {
                // this is a pure Builder class, need to show package to distinguish
                return byKind(kind, type);
            } else {
                return byKind(kind, base.substring(lastIndex + 1) + ".Builder");
            }
        } else {
            return byKind(kind, name);
        }
    }

    private static String byKind(CmOption.Kind kind, String type) {
        // no dots
        return switch (kind) {
            case LIST -> type + "[&#93;";
            case MAP -> "Map&lt;string, " + type + "&gt;";
            default -> type;
        };
    }

    private void resolveMerges() {
        var remaining = new ArrayList<>(configTypes.values());
        var resolvedTypes = new HashMap<String, CmType>();
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < remaining.size(); i++) {
                var next = remaining.get(i);
                boolean resolved = true;
                var options = next.options();
                for (int j = 0; j < options.size(); j++) {
                    var option = options.get(j);
                    var optionType = option.type();
                    if (option.merge()) {
                        // primitives and strings are always resolved
                        if (!(TYPE_MAPPING.containsKey(optionType) || TYPE_MAPPING.containsValue(optionType))) {
                            resolved = false;
                            if (resolvedTypes.containsKey(optionType)) {
                                options.remove(j);
                                options.addAll(resolvedTypes.get(optionType).options());
                                done = false;
                                break;
                            }
                        }
                    }
                }
                if (resolved) {
                    resolvedTypes.put(next.type(), next);
                    remaining.remove(i);
                    done = false;
                    break;
                }
            }
        }

        if (!remaining.isEmpty()) {
            for (var type : remaining) {
                for (var option : type.options()) {
                    if (option.merge()) {
                        LOGGER.log(Level.WARNING, "Option {0}, merges: {1} in {2} (unknown)",
                                option.key(), option.type(), type.annotatedType());
                    }
                }
            }
        }
    }

    private void resolveTypes() {
        var remaining = new ArrayList<>(configTypes.values());
        boolean resolved = true;
        while (resolved) {
            resolved = false;
            for (int i = 0; i < remaining.size(); i++) {
                var next = remaining.get(i);
                if (next.inherits().isEmpty()) {
                    configTypes.put(next.annotatedType(), next);
                    resolved = true;
                    remaining.remove(i);
                    break;
                } else {
                    boolean allExist = true;
                    for (String inherit : next.inherits()) {
                        if (!configTypes.containsKey(inherit)) {
                            allExist = false;
                            break;
                        }
                    }
                    if (allExist) {
                        var resolvedType = resolveType(next);
                        configTypes.put(next.type(), resolvedType);
                        resolved = true;
                        remaining.remove(i);
                        break;
                    }
                }
            }
        }

        if (!remaining.isEmpty()) {
            for (var type : remaining) {
                LOGGER.log(Level.WARNING, "Type {0} inherits contains unknown type(s): {1}", type.type(), type.inherits());
            }
        }
    }

    private CmType resolveType(CmType type) {
        // Allow option info on subclasses or implementations of interfaces to override option info from higher.
        var allOptions = new HashMap<String, CmOption>();

        // Traverse from higher to lower in the inheritance structure so more specific settings take precedence.
        var it = type.inherits().listIterator(type.inherits().size());
        while (it.hasPrevious()) {
            var previous = it.previous();
            for (var e : configTypes.get(previous).options()) {
                allOptions.put(e.key(), e);
            }
        }

        // Now apply options from the type being processed.
        for (CmOption opt : type.options()) {
            allOptions.put(opt.key(), opt);
        }

        return new CmType(
                type.type(),
                type.annotatedType(),
                List.copyOf(allOptions.values()),
                type.description(),
                type.prefix(),
                type.standalone(),
                List.of(),
                type.producers(),
                type.provides());
    }

    private void generateFile(String name, CharSequence content) {
        LOGGER.log(Level.INFO, "Generating " + name);
        try {
            var outputFile = outputDir.resolve(name);
            Files.writeString(outputFile, content, TRUNCATE_EXISTING, CREATE);
            generatedFiles.add(name);
        } catch (IOException ex) {
            LOGGER.log(Level.ERROR, "Failed to generate: " + name, ex);
        }
    }

    private String toJavadocLink(String type) {
        // inner classes must use the ., not /
        StringBuilder link = new StringBuilder();
        String inProgress = type;
        boolean notFirst = false;
        while (true) {
            int dot = inProgress.indexOf('.');
            if (dot == -1) {
                link.append("/");
                break;
            }
            String beforeDot = inProgress.substring(0, dot);
            if (notFirst) {
                link.append("/");

                if (Character.isUpperCase(beforeDot.charAt(0))) {
                    // thw whole segment is class name with an inner class name
                    break;
                }
            }
            link.append(beforeDot);
            inProgress = inProgress.substring(dot + 1);
            notFirst = true;
        }
        if (!inProgress.isBlank()) {
            link.append(inProgress);
        }
        link.append(".html");
        return link.toString();
    }

    private static Template template(Handlebars handlebars, String template) {
        URL resource = ConfigDocs.class.getResource(template);
        if (resource == null) {
            throw new IllegalStateException("Failed to locate required handlebars template on classpath: " + template);
        }
        try {
            return handlebars.compile(new URLTemplateSource(template, resource));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load handlebars template on classpath: " + template, e);
        }
    }

    private static void validateOutputDir(Path outputDir) {
        if (!Files.isDirectory(outputDir)) {
            throw new IllegalArgumentException("Not a directory: " + outputDir);
        }
        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            } else {
                var readme = outputDir.resolve("README.md");
                if (!(Files.exists(readme) && Files.isRegularFile(readme))) {
                    // must be empty
                    try (Stream<Path> stream = Files.list(outputDir)) {
                        if (stream.findAny().isPresent()) {
                            throw new IllegalStateException(
                                    "Directory is not empty and does not contain README.md: " + outputDir);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String fileNameFromTypeName(String typeName) {
        return typeName.replace('.', '_') + ".md";
    }

    private static String titleFromTypeName(String typeName) {
        String title = typeName;
        if (title.startsWith("io.helidon.")) {
            title = title.substring("io.helidon.".length());
            int i = title.lastIndexOf('.');
            if (i != -1) {
                String simpleName = title.substring(i + 1);
                String thePackage = title.substring(0, i);
                title = simpleName + " (" + thePackage + ")";
            }
        }
        return title;
    }

    private static String titleFromFileName(String fileName) {
        var title = fileName;
        if (title.endsWith(".md")) {
            title = title.substring(0, title.length() - 3);
        }
        if (title.startsWith("io_helidon_")) {
            title = title.substring("io_helidon_".length());
            int i = title.lastIndexOf('_');
            if (i != -1) {
                var simpleName = title.substring(i + 1);
                var thePackage = title.substring(0, i);
                title = simpleName + " (" + thePackage.replace('_', '.') + ")";
            }
        }
        return title;
    }
}
