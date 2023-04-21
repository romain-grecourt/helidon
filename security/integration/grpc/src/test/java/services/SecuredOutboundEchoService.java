/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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

package services;

import com.google.protobuf.Descriptors;
import io.helidon.common.context.Contexts;
import io.helidon.common.http.Http;
import io.helidon.nima.grpc.webserver.GrpcService;
import io.helidon.grpc.server.test.Echo;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.security.SecurityContext;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import static io.helidon.grpc.core.ResponseHelper.complete;

/**
 * A simple test gRPC echo service that
 * makes a secure outbound http request.
 */
public class SecuredOutboundEchoService implements GrpcService {

    public Http1Client client;

    @Override
    public Descriptors.FileDescriptor proto() {
        return Echo.getDescriptor();
    }

    @Override
    public void update(Routing routing) {
        routing.unary("Echo", this::echo);
    }

    /**
     * Make a web request passing this method's message parameter and send
     * the web response back to the caller .
     *
     * @param request  the echo request containing the message to echo
     * @param observer the call response
     */
    public void echo(Echo.EchoRequest request, StreamObserver<Echo.EchoResponse> observer) {
        try {
            SecurityContext securityContext = Contexts.context()
                    .flatMap(context -> context.get(SecurityContext.class))
                    .orElseThrow();

            String message = request.getMessage();

            Http1ClientResponse response = client.get("/test")
                                                 .queryParam("message", message)
                                                 // .property(ClientSecurity.PROPERTY_CONTEXT, securityContext) FIXME
                                                 .request();

            if (response.status() == Http.Status.OK_200) {
                String value = response.as(String.class);

                Echo.EchoResponse echoResponse = Echo.EchoResponse.newBuilder().setMessage(value).build();
                complete(observer, echoResponse);
            } else if (response.status() == Http.Status.FORBIDDEN_403
                    || response.status() == Http.Status.UNAUTHORIZED_401) {

                observer.onError(Status.PERMISSION_DENIED.asException());
            } else {
                observer.onError(Status.UNKNOWN.withDescription("Received http response " + response).asException());
            }
        } catch (Throwable thrown) {
            observer.onError(Status.UNKNOWN.withCause(thrown).asException());
        }
    }
}
