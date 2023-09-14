/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Http;
import io.helidon.microprofile.tests.junit5.AddBean;
import io.helidon.microprofile.tests.junit5.HelidonTest;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Test that MP OpenAPI support works when retrieving the OpenAPI document
 * from the server's /openapi endpoint.
 */
@HelidonTest
@AddBean(TestApp.class)
@AddBean(TestApp3.class)
class BasicServerTest {

    @Inject
    private WebTarget webTarget;

    public BasicServerTest() {
    }

    /**
     * Make sure that the annotations in the test app were found and properly
     * incorporated into the OpenAPI document.
     */
    @Test
    public void simpleTest() {
        checkPathValue("paths./testapp/go.get.summary", TestApp.GO_SUMMARY);
    }

    @Test
    public void testMultipleApps() {
        checkPathValue("paths./testapp3/go3.get.summary", TestApp3.GO_SUMMARY);
    }

    private void checkPathValue(String pathExpression, String expected) {
        Map<String, Object> document = fetchDocument();
        String result = TestUtil.fromYaml(document, pathExpression, String.class);
        assertThat(pathExpression, result, is(equalTo(expected)));
    }

    private Map<String, Object> fetchDocument() {
        try (Response response = webTarget.path("/alt-openapi")
                .request(MediaTypes.APPLICATION_OPENAPI_YAML.text()).get()) {

            assertThat(response.getStatus(), CoreMatchers.is(Http.Status.OK_200.code()));
            String yamlText = response.readEntity(String.class);
            return new Yaml().load(yamlText);
        }
    }
}
