package io.helidon.config.metadata.docs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import io.helidon.metadata.hson.Hson;

/**
 * Model {@code config-metadata.json}.
 */
record ConfigMetadata(List<CmModule> modules) {

    private static final String LOCATION = "META-INF/helidon/config-metadata.json";

    ConfigMetadata(Hson.Array array) {
        this(array.getStructs().stream().map(CmModule::new).toList());
    }

    ConfigMetadata(InputStream is) {
        this(Hson.parse(is).asArray());
    }

    /**
     * Load all {@value #LOCATION} from the classpath.
     *
     * @return metadatas
     */
    static List<ConfigMetadata> loadAll() {
        try {
            var metadatas = new ArrayList<ConfigMetadata>();
            var files = ConfigDocs.class.getClassLoader().getResources(LOCATION);
            while (files.hasMoreElements()) {
                URL url = files.nextElement();
                try (InputStream is = url.openStream()) {
                    metadatas.add(new ConfigMetadata(is));
                }
            }
            return metadatas;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Module.
     *
     * @param module module
     * @param types  config types
     */
    record CmModule(String module, List<CmType> types) {
        CmModule(Hson.Struct struct) {
            this(string(struct, "module"), list(struct, "types", CmType::new));
        }
    }

    /**
     * Config type.
     *
     * @param type          type
     * @param annotatedType annotated type
     * @param options       options
     * @param description   description
     * @param prefix        prefix
     * @param standalone    standalone
     * @param inherits      inherits
     * @param producers     producers
     * @param provides      provides
     */
    record CmType(String type,
                  String annotatedType,
                  List<CmOption> options,
                  String description,
                  String prefix,
                  boolean standalone,
                  List<String> inherits,
                  List<String> producers,
                  List<String> provides) {

        CmType(Hson.Struct struct) {
            this(
                    string(struct, "type"),
                    string(struct, "annotatedType"),
                    list(struct, "options", CmOption::new),
                    struct.stringValue("description").orElse(null),
                    struct.stringValue("prefix").orElse(null),
                    struct.booleanValue("standalone").orElse(false),
                    list(struct, "inherits"),
                    list(struct, "producers"),
                    list(struct, "provides"));
        }

        /**
         * Get the short type name.
         *
         * @return short type name
         */
        String shortType() {
            return type.substring(type.lastIndexOf('.'));
        }
    }

    /**
     * Config option.
     *
     * @param key           key
     * @param description   description
     * @param method        method
     * @param type          type
     * @param defaultValue  default value
     * @param required      required
     * @param experimental  experimental
     * @param deprecated    deprecated
     * @param provider      provider
     * @param providerType  provider type
     * @param merge         merge
     * @param kind          kind
     * @param allowedValues allowed values
     */
    record CmOption(
            String key,
            String description,
            String method,
            String type,
            String defaultValue,
            boolean required,
            boolean experimental,
            boolean deprecated,
            boolean provider,
            String providerType,
            boolean merge,
            Kind kind,
            List<CmAllowedValue> allowedValues) {

        CmOption(Hson.Struct struct) {
            this(
                    string(struct, "key"),
                    string(struct, "description"),
                    string(struct, "method"),
                    struct.stringValue("type").orElse("string"),
                    string(struct, "defaultValue"),
                    struct.booleanValue("required").orElse(false),
                    struct.booleanValue("experimental").orElse(false),
                    struct.booleanValue("deprecated").orElse(false),
                    struct.booleanValue("provider").orElse(false),
                    struct.stringValue("providerType").orElse(null),
                    struct.booleanValue("merge").orElse(false),
                    struct.stringValue("kind").map(Kind::valueOf).orElse(Kind.VALUE),
                    list(struct, "allowedValues", CmAllowedValue::new));
        }

        /**
         * Option kind.
         */
        enum Kind {
            /**
             * Single value.
             */
            VALUE,
            /**
             * List of values.
             */
            LIST,
            /**
             * Map of values.
             */
            MAP
        }
    }

    /**
     * Allowed values.
     *
     * @param value       value
     * @param description
     */
    record CmAllowedValue(String value, String description) {
        CmAllowedValue(Hson.Struct struct) {
            this(string(struct, "value"), string(struct, "description"));
        }
    }

    private static String string(Hson.Struct struct, String key) {
        return struct.stringValue(key).orElseThrow(() -> new IllegalStateException(key + " is required"));
    }

    private static <T> List<T> list(Hson.Struct struct, String key, Function<Hson.Struct, T> function) {
        return struct.structArray(key).stream()
                .flatMap(Collection::stream)
                .map(function)
                .toList();
    }

    private static List<String> list(Hson.Struct struct, String key) {
        return struct.stringArray(key).stream()
                .flatMap(Collection::stream)
                .toList();
    }
}
