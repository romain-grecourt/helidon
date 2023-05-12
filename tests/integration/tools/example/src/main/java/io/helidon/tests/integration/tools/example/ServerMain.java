/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.tools.example;

import java.lang.System.Logger.Level;

import io.helidon.logging.common.LogConfig;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;
import io.helidon.nima.webserver.WebServer;

/**
 * Main Class.
 * Sample server application entry point.
 */
public class ServerMain {

    private static final System.Logger LOGGER = System.getLogger(ServerMain.class.getName());
    // Default configuration file name
    private static final String DEFAULT_CONFIG_FILE = "test.yaml";

    /**
     * Main method.
     *
     * @param args command line arguments. 1st argument is configuration file.
     */
    public static void main(String[] args) {

        String configFile;
        if (args != null && args.length > 0) {
            configFile = args[0];
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }
        LOGGER.log(Level.INFO, () -> String.format("Configuration file: %s", configFile));

        LogConfig.configureRuntime();
        startServer(configFile);

    }

    private static WebServer startServer(final String configFile) {
        Config config = Config.create(ConfigSources.classpath(configFile));
        DbClient dbClient = DbClient.builder(config.get("db")).build();
        LifeCycleService lcResource = new LifeCycleService(dbClient);

        WebServer server = WebServer.builder()
                                    .routing(routing -> routing
                                            .register("/LifeCycle", lcResource)
                                            .register("/HelloWorld", new HelloWorldService(dbClient)))
                                    .config(config.get("server"))
                                    .start();

        // Set server instance to exit resource.
        lcResource.setServer(server);

        System.out.printf("WEB server is up! http://localhost:%d/%n", server.port());

        return server;
    }

}
