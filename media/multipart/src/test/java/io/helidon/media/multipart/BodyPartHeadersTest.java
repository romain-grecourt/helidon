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
package io.helidon.media.multipart;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Test {@link BodyPartHeaders}.
 */
public class BodyPartHeadersTest {

    @Test
    public void testHeaderNameCaseInsensitive(){
        BodyPartHeaders headers = BodyPartHeaders.builder()
                .header("content-type", "text/plain")
                .header("Content-ID", "test")
                .header("set-cookie", "sessionid=38afes7a8; HttpOnly; Path=/")
                .header("Set-Cookie", "foo=bar")
                .build();
        assertThat(headers.values("Content-Type"), hasItems("text/plain"));
        assertThat(headers.values("Content-Id"), hasItems("test"));
        assertThat(headers.values("Set-Cookie"),
                hasItems("sessionid=38afes7a8; HttpOnly; Path=/",
                        "foo=bar"));
    }
}
