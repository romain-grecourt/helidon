/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.tests.configprofile;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) {
        LogConfig.configureRuntime();

        Config config = Config.create();

        WebServer server = WebServer.builder()
                                    .config(config.get("server"))
                                    .routing(routing -> routing(routing, config))
                                    .start();

        System.out.println("WEB server is up! http://localhost:" + server.port() + "/greet");
    }

    static void routing(HttpRouting.Builder routing, Config config) {
        GreetService greetService = new GreetService(config);
        routing.register("/greet", greetService);
    }
}
