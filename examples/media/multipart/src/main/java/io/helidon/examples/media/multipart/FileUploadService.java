/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.examples.media.multipart;

import io.helidon.media.multipart.InBoundBodyPart;
import io.helidon.media.multipart.InBoundMultiPart;
import io.helidon.media.multipart.MultiPartSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.util.List;
import java.util.Map.Entry;

/**
 * Exposes an endpoint that handles multipart requests.
 */
public final class FileUploadService implements Service {

    @Override
    public void update(Routing.Rules rules) {
        rules.register(MultiPartSupport.create())
             .post("/plain", this::plain)
             .post("/", this::test1);
    }

    private void plain(ServerRequest req, ServerResponse res) {
        req.content().as(String.class).thenAccept(str -> {
            System.out.println(str);
            res.send();
        });
    }

    private void test1(ServerRequest req, ServerResponse res) {
        req.content().as(InBoundMultiPart.class).thenAccept(multiPart -> {
            for(InBoundBodyPart part : multiPart.bodyParts()){
                System.out.println("\n****** PART ******");
                System.out.println("Headers:");
                for (Entry<String, List<String>> header
                        : part.headers().toMap().entrySet()) {
                    String name = header.getKey();
                    for (String value : header.getValue()) {
                        System.out.println(name + ":" + value);
                    }
                }
                part.content().as(String.class).thenAccept(partContent -> {
                    System.out.println("Content:");
                    System.out.println(partContent);
                });
            }
            res.send();
        });
    }
}
