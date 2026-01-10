/*
 * Copyright (c) 2026 Oracle and/or its affiliates
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
package io.helidon.config.metadata.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.config.metadata.model.CmModelImpl.CmAllowedValueImpl;
import io.helidon.config.metadata.model.CmModelImpl.CmModuleImpl;
import io.helidon.config.metadata.model.CmModelImpl.CmOptionImpl;
import io.helidon.config.metadata.model.CmModelImpl.CmTypeImpl;
import io.helidon.metadata.hson.Hson;

/**
 * Config metadata model.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public interface CmModel {

    /**
     * Classpath location.
     */
    String LOCATION = "META-INF/helidon/config-metadata.json";

    /**
     * Create from JSON.
     *
     * @param jsonArray JSON array
     * @return ConfigMetadata
     */
    static CmModel fromJson(Hson.Array jsonArray) {
        return new CmModelImpl(jsonArray);
    }

    /**
     * Load all {@value #LOCATION} from the classpath.
     *
     * @return metadatas
     */
    static CmModel loadAll(ClassLoader cl) {
        try {
            var structs = new ArrayList<Hson.Struct>();
            var files = cl.getResources(LOCATION);
            while (files.hasMoreElements()) {
                URL url = files.nextElement();
                try (InputStream is = url.openStream()) {
                    structs.addAll(Hson.parse(is).asArray().getStructs());
                }
            }
            return fromJson(Hson.Array.create(structs));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Config modules.
     *
     * @return list of modules
     */
    List<CmModule> modules();

    /**
     * Convert to JSON.
     *
     * @return JSON array, never {@code null}
     */
    Hson.Array toJson();

    /**
     * Config metadata module.
     */
    interface CmModule {
        /**
         * Module name.
         *
         * @return name, never {@code null}
         */
        String module();

        /**
         * Config types.
         *
         * @return types
         */
        List<CmType> types();

        /**
         * Convert to JSON.
         *
         * @return JSON array, never {@code null}
         */
        Hson.Struct toJson();

        /**
         * Create from JSON.
         *
         * @param jsonObject JSON object
         * @return CmModule
         */
        static CmModule fromJson(Hson.Struct jsonObject) {
            return new CmModuleImpl(jsonObject);
        }
    }

    /**
     * Config metadata type.
     */
    interface CmType {

        /**
         * Type name.
         *
         * @return type name, never {@code null}
         */
        String type();

        /**
         * Annotated type name.
         *
         * @return type name
         * @deprecated use {@link #type()} instead
         */
        @Deprecated(since = "4.4.0")
        Optional<String> annotatedType();

        /**
         * Options.
         *
         * @return list of options
         */
        List<CmOption> options();

        /**
         * Description.
         *
         * @return description, never {@code null}
         */
        Optional<String> description();

        /**
         * Optional prefix, if {@link #standalone()} is {@code true}.
         *
         * @return prefix
         */
        Optional<String> prefix();

        /**
         * Indicate whether this is a standalone config object.
         * An optional prefix may be specified with {@link #prefix()}.
         *
         * @return {@code true} if standalone, {@code false} otherwise
         */
        boolean standalone();

        /**
         * Get all the exposed type names.
         *
         * @return list of type names
         */
        List<String> inherits();

        /**
         * Producer methods.
         *
         * @return list of method names
         */
        List<String> producers();

        /**
         * Additional types this type provides.
         *
         * @return list of provider type names
         */
        List<String> provides();

        /**
         * Convert to JSON.
         *
         * @return JSON array, never {@code null}
         */
        Hson.Struct toJson();

        /**
         * Create from JSON.
         *
         * @param jsonObject JSON object
         * @return CmType
         */
        static CmType fromJson(Hson.Struct jsonObject) {
            return new CmTypeImpl(jsonObject);
        }
    }

    /**
     * Config metadata option.
     */
    interface CmOption {

        /**
         * Default value for {@link #type()}.
         */
        String DEFAULT_TYPE = "java.lang.String";

        /**
         * Default value for {@link #kind()}.
         */
        Kind DEFAULT_KIND = Kind.VALUE;

        /**
         * Option kind.
         */
        enum Kind {
            /**
             * Leaf node.
             */
            VALUE,
            /**
             * List tree node.
             */
            LIST,
            /**
             * Map tree node.
             */
            MAP
        }

        /**
         * The key of the config option as used in config.
         *
         * @return key, empty when {@link #merge()} is true
         */
        Optional<String> key();

        /**
         * Description, derived from Javadoc.
         *
         * @return description
         */
        Optional<String> description();

        /**
         * Method.
         *
         * @return method name, never {@code null}
         * @deprecated use {@link CmType#producers() enclosing type producer} instead
         */
        @Deprecated(forRemoval = true, since = "4.4.0")
        Optional<String> method();

        /**
         * The type of the config option.
         * If the type is empty, it is assumed to be {@value #DEFAULT_TYPE}.
         *
         * @return type name
         */
        Optional<String> type();

        /**
         * Default value.
         *
         * @return default value
         */
        Optional<String> defaultValue();

        /**
         * Indicate whether this option is required.
         * If {@code true}, {@link #defaultValue()} is always empty.
         *
         * @return {@code true} if required, {@code false} otherwise
         */
        boolean required();

        /**
         * Indicate whether this option is experimental.
         *
         * @return {@code true} if experimental, {@code false} otherwise
         */
        boolean experimental();

        /**
         * Indicate whether this option is deprecated.
         *
         * @return {@code true} if deprecated, {@code false} otherwise
         */
        boolean deprecated();

        /**
         * Indicate whether this option is abstract and provided by other module(s).
         *
         * @return {@code true} if a provider, {@code false} otherwise
         */
        boolean provider();

        /**
         * The provider interface type name used when {@link #provider()} is {@code true}.
         *
         * @return provider interface type name
         * @deprecated use {@link #type()} instead
         */
        @Deprecated(since = "4.4.0")
        Optional<String> providerType();

        /**
         * Indicate wether to merge the child nodes directly with parent node without a key
         *
         * @return {@code true} to merge, {@code false} otherwise
         */
        boolean merge();

        /**
         * Kind of this option.
         * If the kind is empty, it is assumed to be {@link #DEFAULT_KIND}
         *
         * @return Kind
         */
        Optional<Kind> kind();

        /**
         * Allowed values.
         *
         * @return list of allowed values
         */
        List<CmAllowedValue> allowedValues();

        /**
         * Convert to JSON.
         *
         * @return JSON array, never {@code null}
         */
        Hson.Struct toJson();

        /**
         * Create from JSON.
         *
         * @param jsonObject JSON object
         * @return CmOption
         */
        static CmOption fromJson(Hson.Struct jsonObject) {
            return new CmOptionImpl(jsonObject);
        }
    }

    /**
     * Allowed values.
     */
    interface CmAllowedValue {
        /**
         * Value.
         *
         * @return value, never {@code null}
         */
        String value();

        /**
         * Description.
         *
         * @return description
         */
        Optional<String> description();

        /**
         * Convert to JSON.
         *
         * @return JSON array, never {@code null}
         */
        Hson.Struct toJson();

        /**
         * Create from JSON.
         *
         * @param jsonObject JSON object
         * @return CmAllowedValue
         */
        static CmAllowedValue fromJson(Hson.Struct jsonObject) {
            return new CmAllowedValueImpl(jsonObject);
        }
    }
}
