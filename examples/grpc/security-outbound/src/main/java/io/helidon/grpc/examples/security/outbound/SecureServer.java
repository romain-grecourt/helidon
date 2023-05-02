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

package io.helidon.grpc.examples.security.outbound;

import java.net.InetSocketAddress;
import java.util.Optional;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.grpc.examples.common.Greet;
import io.helidon.grpc.examples.common.StringService;
import io.helidon.grpc.examples.common.StringServiceGrpc;
import io.helidon.grpc.examples.common.Strings;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.grpc.webserver.GrpcRouting;
import io.helidon.nima.grpc.webserver.GrpcService;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.integration.grpc.GrpcClientSecurity;
import io.helidon.security.integration.grpc.GrpcSecurity;
import io.helidon.security.integration.nima.ClientSecurity;
import io.helidon.security.integration.nima.SecurityFeature;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import com.google.protobuf.Descriptors;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.StreamObserver;

import static io.helidon.nima.grpc.webserver.ResponseHelper.complete;

/**
 * An example server that configures services with outbound security.
 */
public class SecureServer {

    private static WebServer webServer;

    private SecureServer() {
    }

    /**
     * Program entry point.
     *
     * @param args the program command line arguments
     */
    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = Config.create();

        Security security = Security.builder()
                                    .addProvider(HttpBasicAuthProvider.create(config.get("http-basic-auth")))
                                    .build();

        webServer = WebServer.builder()
                             .config(config.get("server"))
                             .routing(routing ->
                                     routing.addFeature(SecurityFeature.create(security)
                                                                       .securityDefaults(SecurityFeature.authenticate()))
                                            .register(new RestService()))
                             .addRouting(GrpcRouting.builder()
                                                    // Add the security interceptor with a default of allowing any authenticated user
                                                    .intercept(GrpcSecurity.create(security).securityDefaults(GrpcSecurity.authenticate()))
                                                    // add the StringService with required role "admin"
                                                    .service(new StringService(), GrpcSecurity.rolesAllowed("admin"))
                                                    // add the GreetService (picking up the default security of any authenticated user)
                                                    .service(new GreetService()))
                             .start();
    }

    /**
     * A gRPC greet service that uses outbound security to
     * access a ReST API.
     */
    public static class GreetService implements GrpcService {

        /**
         * The current greeting.
         */
        private String greeting = "hello";

        /**
         * The Helidon WebClient to use to make ReST calls.
         */
        private Http1Client client;

        private GreetService() {
            client = Http1Client.builder()
                                .service(ClientSecurity.create())
                                .build();
        }

        @Override
        public Descriptors.FileDescriptor proto() {
            return Greet.getDescriptor();
        }

        @Override
        public void update(Routing routing) {
            routing.unary("Greet", this::greet)
                   .unary("SetGreeting", this::setGreeting);
        }

        /**
         * This method calls a secure ReST endpoint using the caller's credentials.
         *
         * @param request  the request
         * @param observer the observer to send the response to
         */
        private void greet(Greet.GreetRequest request, StreamObserver<Greet.GreetResponse> observer) {
            // Obtain the greeting name from the request (default to "World".
            String name = Optional.ofNullable(request.getName()).orElse("World");

            // Obtain the security context from the current gRPC context
            SecurityContext securityContext = GrpcSecurity.SECURITY_CONTEXT.get();
            Context context = Context.builder().id("example").build();
            context.register(securityContext);

            Contexts.runInContext(context, () -> {
                // Use the current credentials call the "lower" ReST endpoint which will call
                // the "Lower" method on the secure gRPC StringService.
                Http1ClientResponse response = client.get()
                                                     .uri("http://localhost:" + webServer.port())
                                                     .path("lower")
                                                     .queryParam("value", name)
                                                     .request();

                Http.Status status = response.status();
                if (status == Http.Status.OK_200) {
                    // Send the response to the caller of the current greeting and lower case name
                    String str = response.entity().as(String.class);
                    Greet.GreetResponse build = Greet.GreetResponse.newBuilder().setMessage(str).build();
                    complete(observer, build);
                }

                if (status == Http.Status.UNAUTHORIZED_401 || status == Http.Status.FORBIDDEN_403) {
                    observer.onError(Status.PERMISSION_DENIED.asRuntimeException());
                } else {
                    String str = response.entity().as(String.class);
                    observer.onError(Status.INTERNAL.withDescription(str).asRuntimeException());
                }
            });
        }

        /**
         * This method calls a secure ReST endpoint overriding the caller's credentials and
         * using the admin user's credentials.
         *
         * @param request  the request
         * @param observer the observer to send the response to
         */
        private void setGreeting(Greet.SetGreetingRequest request, StreamObserver<Greet.SetGreetingResponse> observer) {
            // Obtain the greeting name from the request (default to "hello".
            String name = Optional.ofNullable(request.getGreeting()).orElse("hello");

            // Obtain the security context from the current gRPC context
            SecurityContext securityContext = GrpcSecurity.SECURITY_CONTEXT.get();
            Context context = Context.builder().id("example").build();
            context.register(securityContext);

            Contexts.runInContext(context, () -> {
                // Use the current credentials call the "lower" ReST endpoint which will call
                // the "Lower" method on the secure gRPC StringService.
                Http1ClientResponse response = client.get()
                                                     .uri("http://localhost:" + webServer.port())
                                                     .path("lower")
                                                     .queryParam("value", name)
                                                     .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "Ted")
                                                     .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "secret")
                                                     .request();

                Http.Status status = response.status();
                if (status == Http.Status.OK_200) {
                    // Send the response to the caller of the current greeting and lower case name
                    String str = response.entity().as(String.class);
                    Greet.SetGreetingResponse build = Greet.SetGreetingResponse.newBuilder().setGreeting(str).build();
                    complete(observer, build);
                }

                if (status == Http.Status.UNAUTHORIZED_401 || status == Http.Status.FORBIDDEN_403) {
                    observer.onError(Status.PERMISSION_DENIED.asRuntimeException());
                } else {
                    String str = response.entity().as(String.class);
                    observer.onError(Status.INTERNAL.withDescription(str).asRuntimeException());
                }
            });
        }
    }

    /**
     * A ReST service that calls the gRPC StringService to mutate String values.
     */
    public static class RestService implements HttpService {

        private Channel channel;


        @Override
        public void routing(HttpRules rules) {
            rules.get("/lower", SecurityFeature.rolesAllowed("user"), this::lower)
                 .get("/upper", SecurityFeature.rolesAllowed("user"), this::upper);
        }

        /**
         * Call the gRPC StringService Lower method overriding the caller's credentials and
         * using the admin user's credentials.
         *
         * @param req the http request
         * @param res the http response
         */
        private void lower(ServerRequest req, ServerResponse res) {
            try {
                // Create the gRPC client security credentials from the current request
                // overriding with the admin user's credentials
                GrpcClientSecurity clientSecurity = GrpcClientSecurity.builder(req)
                                                                      .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, "Ted")
                                                                      .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, "secret")
                                                                      .build();

                StringServiceGrpc.StringServiceBlockingStub stub = StringServiceGrpc.newBlockingStub(ensureChannel())
                                                                                    .withCallCredentials(clientSecurity);

                String value = req.query().first("value").orElse(null);
                Strings.StringMessage response = stub.lower(Strings.StringMessage.newBuilder().setText(value).build());

                res.status(Http.Status.OK_200).send(response.getText());
            } catch (StatusRuntimeException e) {
                res.status(Http.Status.create(e.getStatus().getCode().value()));
            }
        }

        /**
         * Call the gRPC StringService Upper method using the current caller's credentials.
         *
         * @param req the http request
         * @param res the http response
         */
        private void upper(ServerRequest req, ServerResponse res) {
            try {
                // Create the gRPC client security credentials from the current request
                GrpcClientSecurity clientSecurity = GrpcClientSecurity.create(req);

                StringServiceGrpc.StringServiceBlockingStub stub = StringServiceGrpc.newBlockingStub(ensureChannel())
                                                                                    .withCallCredentials(clientSecurity);

                String value = req.query().first("value").orElse(null);
                Strings.StringMessage response = stub.upper(Strings.StringMessage.newBuilder().setText(value).build());

                res.status(Http.Status.OK_200).send(response.getText());
            } catch (StatusRuntimeException e) {
                res.status(Http.Status.create(e.getStatus().getCode().value()));
            }
        }

        private synchronized Channel ensureChannel() {
            if (channel == null) {
                InetSocketAddress ina = new InetSocketAddress("localhost", webServer.port());
                channel = InProcessChannelBuilder.forAddress(ina).build();
            }
            return channel;
        }
    }
}
