/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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

package io.helidon.security.integration.grpc;

import java.net.InetSocketAddress;
import java.net.URI;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.grpc.core.GrpcHelper;
import io.helidon.grpc.server.test.Echo;
import io.helidon.grpc.server.test.EchoServiceGrpc;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.grpc.webserver.GrpcRouting;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.security.Security;
import io.helidon.security.integration.nima.SecurityFeature;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.SecuredOutboundEchoService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
public class OutboundSecurityIT {

    private static final TestCallCredentials adminCreds = new TestCallCredentials("Ted", "secret");
    private static final SecuredOutboundEchoService echoService = new SecuredOutboundEchoService();
    private static Http1Client client;
    private static EchoServiceGrpc.EchoServiceBlockingStub adminEchoStub;
    private static EchoServiceGrpc.EchoServiceBlockingStub noCredsEchoStub;

    @SetUpServer
    static void setUpServer(WebServer.Builder serverBuilder) {
        LogConfig.configureRuntime();
        Config config = Config.create();
        Security security = Security.builder()
                                    .addProvider(HttpBasicAuthProvider.create(config.get("http-basic-auth")))
                                    .build();

        serverBuilder
                .defaultSocket(builder -> builder
                        .port(-1)
                        .host("localhost"))
                .routing(router -> router
                        .addFeature(SecurityFeature.create(security)
                                                   .securityDefaults(SecurityFeature.authenticate()))
                        .get("/test", SecurityFeature.rolesAllowed("admin"), OutboundSecurityIT::echoWebRequest)
                        .get("/propagate", SecurityFeature.rolesAllowed("user"), OutboundSecurityIT::propagateCredentialsWebRequest)
                        .get("/override", SecurityFeature.rolesAllowed("user"), OutboundSecurityIT::overrideCredentialsWebRequest))
                .addRouting(GrpcRouting.builder()
                                       .service(echoService));
    }

    @BeforeAll
    static void setupStubs(URI uri) {
        InetSocketAddress ina = new InetSocketAddress(uri.getHost(), uri.getPort());
        Channel channel = InProcessChannelBuilder.forAddress(ina).build();

        client = Http1Client.builder().baseUri(uri).build();

        echoService.client = Http1Client.builder().baseUri(uri).build();
        adminEchoStub = EchoServiceGrpc.newBlockingStub(channel).withCallCredentials(adminCreds);
        noCredsEchoStub = EchoServiceGrpc.newBlockingStub(channel);
    }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldMakeSecureOutboundCallFromGrpcMethod() {
        Echo.EchoResponse response = adminEchoStub.echo(Echo.EchoRequest.newBuilder().setMessage("foo").build());
        assertThat(response.getMessage(), is("foo"));
    }

    @Test
    public void shouldPropagateCredentialsToOutboundCallFromWebMethod() {
        String message = "testing...";
        String response = client
                .get("/propagate")
                .queryParam("message", message)
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "Ted") FIXME
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "secret") FIXME
                .request()
                .as(String.class);

        assertThat(response, is(message));
    }

    @Test
    public void shouldPropagateInvalidCredentialsToOutboundCallFromWebMethod() {
        Http1ClientResponse response = client
                .get("/propagate")
                .queryParam("message", "testing...")
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "Bob") FIXME
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "password") FIXME
                .request();

        assertThat(response.status(), is(Http.Status.FORBIDDEN_403));
    }

    @Test
    public void shouldOverrideCredentialsToOutboundCallFromWebMethod() {
        String message = "testing...";
        String response = client
                .get("/override")
                .queryParam("message", message)
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_USERNAME, "Bob") FIXME
                //.property(HttpAuthenticationFeature.HTTP_AUTHENTICATION_BASIC_PASSWORD, "password") FIXME
                .request()
                .as(String.class);

        assertThat(response, is(message));
    }

    // ----- helper methods -------------------------------------------------

    private static void echoWebRequest(ServerRequest req, ServerResponse res) {
        String message = req.query().first("message").orElse(null);
        if (message != null) {
            res.send(message);
        } else {
            res.status(Http.Status.create(401, "missing message query parameter")).send();
        }
    }

    private static void propagateCredentialsWebRequest(ServerRequest req, ServerResponse res) {
        try {
            GrpcClientSecurity clientSecurity = GrpcClientSecurity.create(req);

            EchoServiceGrpc.EchoServiceBlockingStub stub = noCredsEchoStub.withCallCredentials(clientSecurity);

            String message = req.query().first("message").orElse(null);
            Echo.EchoResponse echoResponse = stub.echo(Echo.EchoRequest.newBuilder().setMessage(message).build());
            res.send(echoResponse.getMessage());
        } catch (StatusRuntimeException e) {
            res.status(GrpcHelper.toHttpResponseStatus(e)).send();
        } catch (Throwable thrown) {
            res.status(Http.Status.create(500, thrown.getMessage())).send();
        }
    }

    private static void overrideCredentialsWebRequest(ServerRequest req, ServerResponse res) {
        try {
            GrpcClientSecurity clientSecurity = GrpcClientSecurity.builder(req)
                    .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "Ted")
                    .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "secret")
                    .build();

            EchoServiceGrpc.EchoServiceBlockingStub stub = noCredsEchoStub.withCallCredentials(clientSecurity);

            String message = req.query().first("message").orElse(null);
            Echo.EchoResponse echoResponse = stub.echo(Echo.EchoRequest.newBuilder().setMessage(message).build());
            res.send(echoResponse.getMessage());
        } catch (StatusRuntimeException e) {
            res.status(GrpcHelper.toHttpResponseStatus(e)).send();
        } catch (Throwable thrown) {
            res.status(Http.Status.create(500, thrown.getMessage())).send();
        }
    }
}
