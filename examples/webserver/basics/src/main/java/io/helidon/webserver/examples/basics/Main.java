/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates.
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

package io.helidon.webserver.examples.basics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;

import io.helidon.common.http.Http;
import io.helidon.common.http.HttpException;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.http.ServerRequestHeaders;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.nima.http.media.EntityReader;
import io.helidon.nima.http.media.MediaContext;
import io.helidon.nima.http.media.ReadableEntity;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.ErrorHandler;
import io.helidon.nima.webserver.http.Handler;
import io.helidon.nima.webserver.http.HttpRoute;
import io.helidon.nima.webserver.http.HttpRouting;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.nima.webserver.staticcontent.StaticContentService;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;

/**
 * This example consists of few first tutorial steps of WebServer API. Each step is represented by a single method.
 * <p>
 * <b>Principles:</b>
 * <ul>
 *     <li>Reactive principles</li>
 *     <li>Reflection free</li>
 *     <li>Fluent</li>
 *     <li>Integration platform</li>
 * </ul>
 * <p>
 * It is also java executable main class. Use a method name as a command line parameter to execute.
 */
public class Main {

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    private static final Http.HeaderName BAR_HEADER = Http.Header.create("bar");
    private static final Http.HeaderName FOO_HEADER = Http.Header.create("foo");

    // ---------------- EXAMPLES

    /**
     * True heart of WebServer API is {@link HttpRouting}.
     * It provides fluent way how to assign custom {@link Handler} to the routing rule.
     * The rule consists from two main factors - <i>HTTP method</i> and <i>path pattern</i>.
     * <p>
     * The (route) {@link Handler} is a functional interface which process HTTP {@link ServerRequest request} and
     * writes to the {@link ServerResponse response}.
     */
    public static void firstRouting(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .post("/firstRouting/post-endpoint", (req, res) -> res.status(Http.Status.CREATED_201)
                                                         .send())
                .get("/firstRouting/get-endpoint", (req, res) -> res.status(Http.Status.NO_CONTENT_204)
                                                       .send("Hello World!")));
    }

    /**
     * All routing rules (routes) are evaluated in a definition order. The {@link Handler} assigned with the first valid route
     * for given request is called. It is a responsibility of each handler to process in one of the following ways:
     * <ul>
     *     <li>Respond using one of {@link ServerResponse#send() ServerResponse.send(...)} method.</li>
     *     <li>Continue to next valid route using {@link ServerResponse#next() ServerRequest.next()} method.
     *     <i>It is possible to define filtering handlers.</i></li>
     * </ul>
     * <p>
     * If no valid {@link Handler} is found then routing respond by {@code HTTP 404} code.
     * <p>
     * If selected {@link Handler} doesn't process request than the request <b>stacks</b>!
     * <p>
     * <b>Blocking operations:</b><br>
     * For performance reason, {@link Handler} can be called directly by a selector thread. It is not good idea to block
     * such thread. If request must be processed by a blocking operation then such processing should be deferred to another
     * thread.
     */
    public static void routingAsFilter(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .route(HttpRoute.builder())
                .any("/routingAsFilter/*", (req, res) -> {
                    System.out.println(req.prologue().method() + " " + req.path());
                    // Filters are just routing handlers which calls next()
                    res.next();
                })
                .post("/routingAsFilter/post-endpoint", (req, res) -> res.status(Http.Status.CREATED_201)
                                                         .send())
                .get("/routingAsFilter/get-endpoint", (req, res) -> res.status(Http.Status.NO_CONTENT_204)
                                                       .send("Hello World!")));
    }

    /**
     * {@link ServerRequest} provides access to three types of "parameters":
     * <ul>
     *     <li>Headers</li>
     *     <li>Query parameters</li>
     *     <li>Path parameters - <i>Evaluated from provided {@code path pattern}</i></li>
     * </ul>
     * <p>
     * {@link java.util.Optional Optional} API is heavily used to represent parameters optionality.
     * <p>
     * WebServer {@link io.helidon.common.parameters.Parameters Parameters} API is used to represent fact, that <i>headers</i> and
     * <i>query parameters</i> can contain multiple values.
     */
    public static void parametersAndHeaders(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .get("/parametersAndHeaders/context/{id}", (req, res) -> {
                    StringBuilder sb = new StringBuilder();
                    // Request headers
                    req.headers()
                       .first(FOO_HEADER)
                       .ifPresent(v -> sb.append("foo: ").append(v).append("\n"));
                    // Request parameters
                    req.query()
                       .first("bar")
                       .ifPresent(v -> sb.append("bar: ").append(v).append("\n"));
                    // Path parameters
                    sb.append("id: ").append(req.path().pathParameters().value("id"));
                    // Response headers
                    res.headers().contentType(MediaTypes.TEXT_PLAIN);
                    // Response entity (payload)
                    res.send(sb.toString());
                }));
    }

    /**
     * Routing rules (routes) are limited on two criteria - <i>HTTP method and path</i>.
     */
    public static void advancedRouting(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .get("/advancedRouting/foo", (req, res) -> {
                    ServerRequestHeaders headers = req.headers();
                    if (headers.isAccepted(HttpMediaType.TEXT_PLAIN)
                            && headers.contains(BAR_HEADER)) {

                        res.send();
                    } else {
                        res.next();
                    }
                }));
    }

    /**
     * Larger applications with many routing rules can cause complicated readability (maintainability) if all rules are
     * defined in a single fluent code. It is possible to register {@link HttpService} and organise
     * the code into services and resources. {@code Service} is an interface which can register more routing rules (routes).
     */
    public static void organiseCode(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .register("/organiseCode/catalog-context-path", new Catalog()));
    }

    /**
     * Request payload (body/entity) is represented by {@link ReadableEntity}.
     * But it is more convenient to process entity in some type specific form. WebServer supports few types which can be
     * used te read the whole entity:
     * <ul>
     *     <li>{@code byte[]}</li>
     *     <li>{@code String}</li>
     *     <li>{@code InputStream}</li>
     * </ul>
     * <p>
     * Similar approach is used for the response entity.
     */
    public static void readContentEntity(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .post("/readContentEntity/foo", (req, res) -> {
                    try {
                        String data = req.content().as(String.class);
                        System.out.println("/foo DATA: " + data);
                        res.send(data);
                    } catch (Throwable th) {
                        res.status(Http.Status.BAD_REQUEST_400);
                    }
                })
                // It is possible to use Handler.of() method to automatically cover all error states.
                .post("/readContentEntity/bar", Handler.create(String.class, (data, res) -> {
                    System.out.println("/foo DATA: " + data);
                    res.send(data);
                })));
    }

    /**
     * Use a custom {@link EntityReader reader} to convert the request content into an object of a given type.
     */
    public static void mediaReader(WebServer.Builder builder) {
        builder.routing(routing -> routing
                       .post("/mediaReader/create-record", Handler.create(Name.class, (name, res) -> {
                           System.out.println("Name: " + name);
                           res.status(Http.Status.CREATED_201)
                              .send(name.toString());
                       })))
               // Create a media support that contains the defaults and our custom Name reader
               .mediaContext(MediaContext.builder()
                                         .addMediaSupport(NameSupport.create()));
    }

    /**
     * Combination of filtering {@link Handler} pattern with {@link HttpService} registration capabilities
     * can be used by other frameworks for the integration. WebServer is shipped with several integrated libraries (supports)
     * including <i>static content</i>, JSON and Jersey. See {@code POM.xml} for requested dependencies.
     */
    public static void supports(WebServer.Builder builder) {
        builder.routing(routing -> routing
                       .register(StaticContentService.create("/static"))
                       .get("/supports/hello/{what}", (req, res) ->
                               res.send(JSON.createObjectBuilder()
                                            .add("message", "Hello " + req.path()
                                                                          .pathParameters()
                                                                          .value("what"))
                                            .build())))
               .mediaContext(MediaContext.builder()
                                         .addMediaSupport(JsonpSupport.create()));
    }

    /**
     * Request processing can cause error represented by {@link Throwable}. It is possible to register custom
     * {@link ErrorHandler ErrorHandlers} for specific processing.
     * <p>
     * If error is not processed by a custom {@link ErrorHandler ErrorHandler} than default one is used.
     * It respond with <i>HTTP 500 code</i> unless error is not represented
     * by {@link HttpException HttpException}. In such case it reflects its content.
     */
    public static void errorHandling(WebServer.Builder builder) {
        builder.routing(routing -> routing
                .post("/errorHandling/compute", Handler.create(String.class, (str, res) -> {
                    int result = 100 / Integer.parseInt(str);
                    res.send("100 / " + str + " = " + result);
                }))
                .error(Throwable.class, (req, res, ex) -> {
                    ex.printStackTrace(System.out);
                    res.next();
                })
                .error(NumberFormatException.class,
                        (req, res, ex) -> res.status(Http.Status.BAD_REQUEST_400).send())
                .error(ArithmeticException.class,
                        (req, res, ex) -> res.status(Http.Status.PRECONDITION_FAILED_412).send()));
    }


    // ---------------- EXECUTION

    private static final String SYSPROP_EXAMPLE_NAME = "exampleName";
    private static final String ENVVAR_EXAMPLE_NAME = "EXAMPLE_NAME";

    /**
     * Prints usage instructions.
     */
    public void help() {
        StringBuilder hlp = new StringBuilder();
        hlp.append("java -jar example-basics.jar <exampleMethodName>\n");
        hlp.append("Example method names:\n");
        Method[] methods = Main.class.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                hlp.append("    ").append(method.getName()).append('\n');
            }
        }
        hlp.append('\n');
        hlp.append("Example method name can be also provided as a\n");
        hlp.append("    - -D").append(SYSPROP_EXAMPLE_NAME).append(" jvm property.\n");
        hlp.append("    - ").append(ENVVAR_EXAMPLE_NAME).append(" environment variable.\n");
        System.out.println(hlp);
    }

    /**
     * Prints usage instructions. (Shortcut to {@link #help()} method.
     */
    public void h() {
        help();
    }

    /**
     * Java main method.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        String exampleName = null;
        if (args.length > 0) {
            exampleName = args[0];
        } else if (System.getProperty(SYSPROP_EXAMPLE_NAME) != null) {
            exampleName = System.getProperty(SYSPROP_EXAMPLE_NAME);
        } else if (System.getenv(ENVVAR_EXAMPLE_NAME) != null) {
            exampleName = System.getenv(ENVVAR_EXAMPLE_NAME);
        } else {
            System.out.println("Missing example name. It can be provided as a \n"
                    + "    - first command line argument.\n"
                    + "    - -D" + SYSPROP_EXAMPLE_NAME + " jvm property.\n"
                    + "    - " + ENVVAR_EXAMPLE_NAME + " environment variable.\n");
            System.exit(1);
        }
        while (exampleName.startsWith("-")) {
            exampleName = exampleName.substring(1);
        }
        try {
            Method method = Main.class.getMethod(exampleName);
            WebServer.Builder builder = WebServer.builder();
            method.invoke(null, builder);
            WebServer server = builder.start();
            System.out.println("Server is UP: http://localhost:" + server.port());
        } catch (NoSuchMethodException e) {
            System.out.println("Missing example method named: " + exampleName);
            System.exit(2);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace(System.out);
            System.exit(100);
        }
    }
}
