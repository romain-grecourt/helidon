/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.microprofile.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

import io.helidon.openapi.OpenApiFormat;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class AdditionalPropertiesTest {

    @Test
    void checkParsingBooleanAdditionalProperties() throws IOException {
        OpenAPI openAPI = parse("/withBooleanAddlProps.yml");
        Schema itemSchema = openAPI.getComponents().getSchemas().get("item");

        Schema additionalPropertiesSchema = itemSchema.getAdditionalPropertiesSchema();
        Boolean additionalPropertiesBoolean = itemSchema.getAdditionalPropertiesBoolean();

        assertThat(additionalPropertiesSchema, is(nullValue()));
        assertThat(additionalPropertiesBoolean, is(notNullValue()));
        assertThat(additionalPropertiesBoolean, is(false));
    }

    @Test
    void checkParsingSchemaAdditionalProperties() throws IOException {
        OpenAPI openAPI = parse("/withSchemaAddlProps.yml");
        Schema itemSchema = openAPI.getComponents().getSchemas().get("item");

        Schema additionalPropertiesSchema = itemSchema.getAdditionalPropertiesSchema();
        Boolean additionalPropertiesBoolean = itemSchema.getAdditionalPropertiesBoolean();

        assertThat(additionalPropertiesBoolean, is(nullValue()));
        assertThat(additionalPropertiesSchema, is(notNullValue()));

        Map<String, Schema> additionalProperties = additionalPropertiesSchema.getProperties();
        assertThat(additionalProperties, hasKey("code"));
        assertThat(additionalProperties, hasKey("text"));
    }

    @Test
    void checkWritingSchemaAdditionalProperties() throws IOException {
        OpenAPI openAPI = parse("/withSchemaAddlProps.yml");
        String document = format(openAPI);

        // Expected output:
        //    additionalProperties:
        //        type: object
        //        properties:
        //          code:
        //            type: integer
        //          text:
        //            type: string
        Yaml yaml = new Yaml();
        Map<String, Object> model = yaml.load(document);
        Map<String, ?> item = asMap(model, "components", "schemas", "item");

        Object additionalProperties = item.get("additionalProperties");

        assertThat(additionalProperties, is(instanceOf(Map.class)));

    }

    @Test
    void checkWritingBooleanAdditionalProperties() throws IOException {
        OpenAPI openAPI = parse("/withBooleanAddlProps.yml");
        String document = format(openAPI);

        assertThat(document, containsString("additionalProperties: false"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> asMap(Map<String, ?> map, String... keys) {
        Map<String, ?> m = map;
        for (String key : keys) {
            m = (Map<String, ?>) m.get(key);
        }
        return m;
    }

    private static String format(OpenAPI model) {
        StringWriter sw = new StringWriter();
        OpenApiSerializer.serialize(OpenApiHelper.types(), model, OpenApiFormat.YAML, sw);
        return sw.toString();
    }

    private static OpenAPI parse(String path) throws IOException {
        try (InputStream is = AdditionalPropertiesTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return OpenApiParser.parse(OpenApiHelper.types(), OpenAPI.class, new InputStreamReader(is));
        }
    }
}