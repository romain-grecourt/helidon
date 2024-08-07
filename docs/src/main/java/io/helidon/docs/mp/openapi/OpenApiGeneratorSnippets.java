/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.docs.mp.openapi;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@SuppressWarnings("ALL")
class OpenApiGeneratorSnippets {

    /*
        # Instructions to generate a project containing the types which need to be mocked.
        # In case of changes in the upstream generator.
        # See https://github.com/OpenAPITools/openapi-generator

        # download the openapi-generator cli
        curl -O \
            --output-dir /tmp \
            https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/7.7.0/openapi-generator-cli-7.7.0.jar

        # generate the petapi client project (replace helidonVersion accordingly)
        java -jar /tmp/openapi-generator-cli-7.7.0.jar generate \
           -o /tmp/petapi-client2 \
           -g java-helidon-client \
           --library mp \
           -i etc/petstorex.yaml \
           -p helidonVersion=4.0.11
    */

    // stub
    interface Pet {
    }

    // stub
    interface PetApi {
        Pet getPetById(long petId);
    }

    // stub
    class ApiException extends Exception {
    }

    class Snippet1 {
        // tag::snippet_1[]
        @Path("/exampleServiceCallingService") // <1>
        public class ExampleOpenApiGenClientResource {
            @Inject // <2>
            @RestClient // <3>
            private PetApi petApi; // <4>

            @GET
            @Path("/getPet/{petId}")
            @Produces(MediaType.APPLICATION_JSON)
            public Pet getPetUsingId(@PathParam("petId") Long petId) throws ApiException {
                Pet pet = petApi.getPetById(petId); // <5>
                return pet;
            }
        }
        // end::snippet_1[]
    }

}