/*
 * Copyright (c) 2017, 2023 Oracle and/or its affiliates.
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

package io.helidon.webserver.examples.staticcontent;

import io.helidon.common.http.Http;
import io.helidon.nima.http.media.jsonp.JsonpSupport;
import io.helidon.nima.webserver.Routing;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;
import io.helidon.nima.webserver.staticcontent.StaticContentService;

/**
 * Application demonstrates combination of the static content with a simple REST API. It counts accesses and display it
 * on the WEB page.
 */
public class Main {
    private static final Http.HeaderValue UI_REDIRECT = Http.Header.createCached(Http.Header.LOCATION, "/ui");

    private Main() {
    }

    /**
     * A java main class.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        WebServer server = WebServer.builder()
                                    .port(8080)
                                    .routing(Main::createRouting)
                                    .addMediaSupport(JsonpSupport.create())
                                    .start();

        System.out.println("WEB server is up! http://localhost:" + server.port());
    }

    /**
     * Creates new {@link Routing}.
     *
     * @return the new instance
     */
    static Routing createRouting(HttpRouting.Builder routing) {
        return routing.any("/", (req, res) -> {
                          // showing the capability to run on any path, and redirecting from root
                          res.status(Http.Status.MOVED_PERMANENTLY_301);
                          res.headers().set(UI_REDIRECT);
                          res.send();
                      })
                      .register("/ui", new CounterService())
                      .register("/ui", StaticContentService.builder("web")
                                                           .welcomeFileName("index.html")
                                                           .build())
                      .build();
    }
}
