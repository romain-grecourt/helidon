/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

package io.helidon.tests.integration.nativeimage.se1;

import java.io.IOException;
import java.lang.System.Logger.Level;

import io.helidon.nima.websocket.WsListener;
import io.helidon.nima.websocket.WsSession;


public class WebSocketEndpoint implements WsListener {

    private static final System.Logger LOGGER = System.getLogger(WebSocketEndpoint.class.getName());

    private StringBuilder sb = new StringBuilder();

    @Override
    public void onMessage(WsSession session, String text, boolean last) {
        LOGGER.log(Level.INFO, "WS Receiving " + text);
        if (text.contains("SEND")) {
            session.send(sb.toString(), false);
            sb.setLength(0);
        } else {
            sb.append(text);
        }
    }

    @Override
    public void onOpen(WsSession session) {
        LOGGER.log(Level.INFO, "Session " + session);
    }
}
