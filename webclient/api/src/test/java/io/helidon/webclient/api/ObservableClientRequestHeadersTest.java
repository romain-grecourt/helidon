/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.webclient.api;

import java.util.ArrayList;
import java.util.List;

import io.helidon.http.ClientRequestHeaders;
import io.helidon.http.HeaderChange;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.ObservableHeaders;
import io.helidon.http.WritableHeaders;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservableClientRequestHeadersTest {
    private static final HeaderName FIRST = HeaderNames.create("X-First");
    private static final HeaderName SECOND = HeaderNames.create("X-Second");

    @Test
    void testMutationEvents() {
        ObservableClientRequestHeaders headers = observableHeaders();
        List<HeaderChange> changes = new ArrayList<>();
        headers.addListener(changes::add);

        headers.add(HeaderValues.create(FIRST, "one"));
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(HeaderChange.Added.class));
        assertThat(((HeaderChange.Added) changes.get(0)).current().get(), is("one"));

        headers.add(HeaderValues.create(FIRST, "two"));
        assertThat(changes.size(), is(2));
        assertThat(changes.get(1), instanceOf(HeaderChange.Replaced.class));
        HeaderChange.Replaced addedValue = (HeaderChange.Replaced) changes.get(1);
        assertThat(addedValue.previous().get(), is("one"));
        assertThat(addedValue.current().allValues(), is(List.of("one", "two")));

        headers.set(HeaderValues.create(FIRST, "three"));
        assertThat(changes.size(), is(3));
        assertThat(changes.get(2), instanceOf(HeaderChange.Replaced.class));
        HeaderChange.Replaced replaced = (HeaderChange.Replaced) changes.get(2);
        assertThat(replaced.previous().allValues(), is(List.of("one", "two")));
        assertThat(replaced.current().get(), is("three"));

        headers.remove(FIRST);
        assertThat(changes.size(), is(4));
        assertThat(changes.get(3), instanceOf(HeaderChange.Removed.class));
        assertThat(((HeaderChange.Removed) changes.get(3)).previous().get(), is("three"));

        headers.clear();
        assertThat(changes.size(), is(4));

        headers.set(HeaderValues.create(FIRST, "four"));
        changes.clear();
        headers.clear();
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(HeaderChange.Cleared.class));
        assertThat(((HeaderChange.Cleared) changes.get(0)).previous().get(FIRST).get(), is("four"));
    }

    @Test
    void testSetIfAbsentEvents() {
        ObservableClientRequestHeaders headers = observableHeaders();
        List<HeaderChange> changes = new ArrayList<>();
        headers.addListener(changes::add);

        headers.setIfAbsent(HeaderValues.create(FIRST, "one"));
        headers.setIfAbsent(HeaderValues.create(FIRST, "two"));

        assertThat(headers.get(FIRST).get(), is("one"));
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(HeaderChange.Added.class));
    }

    @Test
    void testFromEvents() {
        ObservableClientRequestHeaders headers = observableHeaders();
        List<HeaderChange> changes = new ArrayList<>();
        headers.addListener(changes::add);

        headers.set(HeaderValues.create(FIRST, "one"));
        changes.clear();

        WritableHeaders<?> replacement = WritableHeaders.create();
        replacement.set(HeaderValues.create(FIRST, "two"));
        headers.from(replacement);
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(HeaderChange.Replaced.class));

        changes.clear();
        WritableHeaders<?> addition = WritableHeaders.create();
        addition.set(HeaderValues.create(SECOND, "three"));
        headers.from(addition);
        assertThat(changes.size(), is(1));
        assertThat(changes.get(0), instanceOf(HeaderChange.Added.class));
    }

    @Test
    void testRemoveListenerStopsEvents() {
        ObservableClientRequestHeaders headers = observableHeaders();
        List<HeaderChange> changes = new ArrayList<>();
        HeaderChange.Listener listener = changes::add;

        headers.addListener(listener);
        headers.removeListener(listener);
        headers.set(HeaderValues.create(FIRST, "one"));

        assertThat(changes.size(), is(0));
    }

    @Test
    void testMirrorWhile() {
        ObservableClientRequestHeaders source = observableHeaders();
        ClientRequestHeaders target = ClientRequestHeaders.create(WritableHeaders.create());

        String result = ObservableHeaders.mirrorWhile(source, target, () -> {
            source.set(HeaderValues.create(FIRST, "one"));
            source.add(HeaderValues.create(FIRST, "two"));
            source.set(HeaderValues.create(SECOND, "three"));
            source.remove(FIRST);
            return "done";
        });

        assertThat(result, is("done"));
        assertThat(target.contains(FIRST), is(false));
        assertThat(target.get(SECOND).get(), is("three"));

        source.set(HeaderValues.create(FIRST, "after"));
        assertThat(target.contains(FIRST), is(false));
    }

    @Test
    void testMirrorWhileDirectActionWhenSourceIsNotObservable() {
        ClientRequestHeaders source = ClientRequestHeaders.create(WritableHeaders.create());
        ClientRequestHeaders target = ClientRequestHeaders.create(WritableHeaders.create());

        String result = ObservableHeaders.mirrorWhile(source, target, () -> {
            source.set(HeaderValues.create(FIRST, "one"));
            return "done";
        });

        assertThat(result, is("done"));
        assertThat(target.contains(FIRST), is(false));
    }

    @Test
    void testMirrorWhileRemovesListenerWhenActionFails() {
        ObservableClientRequestHeaders source = observableHeaders();
        ClientRequestHeaders target = ClientRequestHeaders.create(WritableHeaders.create());

        assertThrows(IllegalStateException.class, () -> source.mirrorWhile(target, () -> {
            source.set(HeaderValues.create(FIRST, "one"));
            throw new IllegalStateException("expected");
        }));

        assertThat(target.get(FIRST).get(), is("one"));
        source.set(HeaderValues.create(FIRST, "after"));
        assertThat(target.get(FIRST).get(), is("one"));
    }

    @Test
    void testNullsRejected() {
        ObservableClientRequestHeaders headers = observableHeaders();
        ClientRequestHeaders target = ClientRequestHeaders.create(WritableHeaders.create());
        ClientRequestHeaders nonObservable = ClientRequestHeaders.create(WritableHeaders.create());

        assertThrows(NullPointerException.class, () -> headers.addListener(null));
        assertThrows(NullPointerException.class, () -> headers.removeListener(null));
        assertThrows(NullPointerException.class, () -> ObservableHeaders.mirrorWhile(null, target, () -> null));
        assertThrows(NullPointerException.class, () -> ObservableHeaders.mirrorWhile(nonObservable, null, () -> null));
        assertThrows(NullPointerException.class, () -> ObservableHeaders.mirrorWhile(nonObservable, target, null));
        assertThrows(NullPointerException.class, () -> headers.mirrorWhile(null, () -> null));
        assertThrows(NullPointerException.class, () -> headers.mirrorWhile(target, null));
        assertThrows(NullPointerException.class, () -> new HeaderChange.Added(null));
        assertThrows(NullPointerException.class, () -> new HeaderChange.Replaced(HeaderValues.create(FIRST, "one"), null));
        assertThrows(NullPointerException.class, () -> new HeaderChange.Removed(null));
        assertThrows(NullPointerException.class, () -> new HeaderChange.Cleared(null));
    }

    private static ObservableClientRequestHeaders observableHeaders() {
        return new ObservableClientRequestHeaders(ClientRequestHeaders.create(WritableHeaders.create()));
    }
}
