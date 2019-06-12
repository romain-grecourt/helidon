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

import java.util.Collection;

/**
 * In-bound multipart message.
 */
public final class InBoundMultiPart extends MultiPart<InBoundBodyPart> {

    /**
     * Create a new in-bound multipart message.
     * @param parts body parts
     */
    InBoundMultiPart(Collection<InBoundBodyPart> parts) {
        super(parts);
    }
}
