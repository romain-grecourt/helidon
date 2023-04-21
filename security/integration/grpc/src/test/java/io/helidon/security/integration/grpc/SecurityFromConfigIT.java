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
import java.util.Iterator;

import io.helidon.config.Config;
import io.helidon.grpc.server.test.Echo;
import io.helidon.grpc.server.test.EchoServiceGrpc;
import io.helidon.grpc.server.test.StringServiceGrpc;
import io.helidon.grpc.server.test.Strings;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.grpc.webserver.GrpcRouting;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webserver.WebServer;
import io.helidon.security.Security;
import io.helidon.security.integration.nima.SecurityFeature;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import services.EchoService;
import services.StringService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpellCheckingInspection")
@ServerTest
class SecurityFromConfigIT {

    private static final TestCallCredentials adminCreds = new TestCallCredentials("Ted", "secret");
    private static final TestCallCredentials userCreds = new TestCallCredentials("Bob", "password");
    private static EchoServiceGrpc.EchoServiceBlockingStub adminEchoStub;
    private static EchoServiceGrpc.EchoServiceBlockingStub userEchoStub;
    private static StringServiceGrpc.StringServiceBlockingStub adminStringStub;
    private static StringServiceGrpc.StringServiceBlockingStub userStringStub;
    private static StringServiceGrpc.StringServiceBlockingStub noCredsEchoStub;

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
                                                   .securityDefaults(SecurityFeature.authenticate())))
                .addRouting(GrpcRouting.builder()
                                       .service(new EchoService())
                                       .service(new StringService()));
    }

    @BeforeAll
    static void setupStubs(URI uri) {
        InetSocketAddress ina = new InetSocketAddress(uri.getHost(), uri.getPort());
        Channel channel = InProcessChannelBuilder.forAddress(ina).build();

        adminEchoStub = EchoServiceGrpc.newBlockingStub(channel).withCallCredentials(adminCreds);
        userEchoStub = EchoServiceGrpc.newBlockingStub(channel).withCallCredentials(userCreds);
        adminStringStub = StringServiceGrpc.newBlockingStub(channel).withCallCredentials(adminCreds);
        userStringStub = StringServiceGrpc.newBlockingStub(channel).withCallCredentials(userCreds);
        noCredsEchoStub = StringServiceGrpc.newBlockingStub(channel);
    }

    /**
     * The StringService lower method is secured at the global default level
     * to allow any authenticated user so should allow access to user Bob.
     */
    @Test
    public void shouldBeSecuredWithGlobalSettingsAllowAccess() {
        Strings.StringMessage message = userStringStub.lower(toMessage("ABCD"));
        assertThat(message.getText(), is("abcd"));
    }

    /**
     * The StringService lower method is secured at the global default level
     * to allow any authenticated user so should disallow access to access
     * without credentials.
     */
    @Test
    public void shouldBeSecuredWithGlobalSettingsDenyAccess() {
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () ->
                noCredsEchoStub.lower(toMessage("FOO")));

        assertThat(thrown.getStatus().getCode(), is(Status.PERMISSION_DENIED.getCode()));
    }

    /**
     * The EchoService echo method is secured roles-allowed = ["admin"] at the service level
     * so should allow user Ted access.
     */
    @Test
    public void shouldBeSecuredWithServiceSettingsAllowAccess() {
        Echo.EchoResponse response = adminEchoStub.echo(Echo.EchoRequest.newBuilder().setMessage("foo").build());
        assertThat(response.getMessage(), is("foo"));
    }

    /**
     * The EchoService echo method is secured roles-allowed = ["admin"] at the service level
     * so should deny user Bob access.
     */
    @Test
    public void shouldBeSecuredWithServiceSettingsDenyAccess() {
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () ->
                userEchoStub.echo(Echo.EchoRequest.newBuilder().setMessage("foo").build()));

        assertThat(thrown.getStatus().getCode(), is(Status.PERMISSION_DENIED.getCode()));
    }

    /**
     * The StringService upper method is secured with roles-allowed = ["admin"]
     * so should allow user Ted access.
     */
    @Test
    public void shouldBeSecuredWithMethodSettingsAllowAccess() {
        Strings.StringMessage message = adminStringStub.upper(toMessage("abcd"));
        assertThat(message.getText(), is("ABCD"));
    }

    /**
     * The StringService split method is secured with roles-allowed = ["admin"]
     * so should deny user Bob access.
     */
    @Test
    public void shouldBeSecuredWithMethodSettingsDenyAccess() {
        // StringService.split is a server streaming call so the proto generated code will
        // return an Iterator even though the actual call fails with PERMISSION_DENIED
        Iterator<Strings.StringMessage> it = userStringStub.split(toMessage("a b c d"));

        // It is not until accessing methods on the Iterator that we get the exception
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, it::hasNext);

        assertThat(thrown.getStatus().getCode(), is(Status.PERMISSION_DENIED.getCode()));
    }

    private Strings.StringMessage toMessage(String text) {
        return Strings.StringMessage.newBuilder().setText(text).build();
    }
}
