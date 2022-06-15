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
package io.helidon.media.common;

import java.util.List;
import java.util.Optional;

import io.helidon.common.http.FormParams;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link FormSupport}.
 */
class FormSupportTest {

    private static final String KEY1 = "key1";
    private static final String VAL1 = "value1";

    private static final String KEY2 = "key2";
    private static final String VAL2_1 = "value2.1";
    private static final String VAL2_2 = "value2.2";

    @Test
    void testOneLineSingleAssignment() {
        FormParams fp = FormSupport.readTextPlain(KEY1 + "=" + VAL1);
        checkKey1(fp);
    }

    @Test
    void testTwoDifferentAssignments() {
        String data = String.format("%s=%s\n%s=%s\n%s=%s", KEY1, VAL1, KEY2, VAL2_1, KEY2, VAL2_2);
        FormParams fp = FormSupport.readTextPlain(data);

        checkKey1(fp);
        checkKey2(fp);
    }

    @Test
    void testTwoDifferentAssignmentsURLEncoded() {
        String data = String.format("%s=%s&%s=%s&%s=%s", KEY1, VAL1, KEY2, VAL2_1, KEY2, VAL2_2);

        FormParams fp = FormSupport.readURLEncoded(data, UTF_8);

        checkKey1(fp);
        checkKey2(fp);
    }

    @Test
    void testAbsentKey() {
        FormParams fp = FormSupport.readTextPlain(KEY1 + "=" + VAL1);

        Optional<String> shouldNotExist = fp.first(KEY2);
        assertThat(shouldNotExist.isPresent(), is(false));

        List<String> shouldBeEmpty = fp.all(KEY2);
        assertThat(shouldBeEmpty.size(), is(0));

        assertThrows(UnsupportedOperationException.class, () -> fp.computeSingleIfAbsent(KEY2, k -> "replacement"));
    }

    private static void checkKey1(FormParams fp) {
        Optional<String> result = fp.first(KEY1);
        assertThat(result.orElse("missing"), is(equalTo(VAL1)));

        List<String> listResult = fp.all(KEY1);
        assertThat(listResult, hasItem(VAL1));
        assertThat(listResult.size(), is(1));
    }

    private static void checkKey2(FormParams fp) {
        Optional<String> result = fp.first(KEY2);
        assertThat(result.orElse("missing"), is(equalTo(VAL2_1)));

        List<String> listResult = fp.all(KEY2);
        assertThat(listResult, hasItems(VAL2_1, VAL2_2));
    }

}
