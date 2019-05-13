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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import io.helidon.common.http.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * MIMEParser test.
 */
public class MIMEParserTest {

    @Test
    public void testBoundaryWhiteSpace() throws IOException {
        String boundary = "boundary";
        final byte[] chunk1 = ("--" + boundary + "\n"
                + "Content-Id: part1\n"
                + "\n"
                + "1\n"
                + "--" + boundary + "\n"
                + "Content-Id: part2\n"
                + "\n"
                + "2\n"
                + "--" + boundary + "--   ").getBytes();
        List<MIMEPart> parts = parseParts("boundary", chunk1);
        assertThat(parts.size(), is(equalTo(2)));

        MIMEPart part1 = parts.get(0);
        assertThat(part1.headers, is(notNullValue()));
        assertThat(part1.headers.get("Content-Id"), hasItems("part1"));
        assertThat(part1.content, is(notNullValue()));
        assertThat(new String(part1.content), is(equalTo("1")));

        MIMEPart part2 = parts.get(1);
        assertThat(part2.headers, is(notNullValue()));
        assertThat(part2.headers.get("Content-Id"), hasItems("part2"));
        assertThat(part2.content, is(notNullValue()));
        assertThat(new String(part2.content), is(equalTo("2")));
    }

    @Test
    public void testMsg() throws IOException {
        String boundary = "----=_Part_4_910054940.1065629194743";
        final byte[] chunk1 = concat(("--" + boundary + "\n"
                + "Content-Type: text/xml; charset=UTF-8\n"
                + "Content-Transfer-Encoding: binary\n"
                + "Content-Id: part1\n"
                + "Content-Description:   this is part1\n"
                + "\n"
                + "<foo>bar</foo>\n"
                + "--" + boundary + "\n"
                + "Content-Type: image/jpeg\n"
                + "Content-Transfer-Encoding: binary\n"
                + "Content-Id: part2\n"
                + "\n").getBytes(),
                new byte[] { (byte)0xff, (byte)0xd8 },
                ("\n--" + boundary + "--").getBytes());

        List<MIMEPart> parts = parseParts(boundary, chunk1);
        assertThat(parts.size(), is(equalTo(2)));

        MIMEPart part1 = parts.get(0);
        assertThat(part1.headers, is(notNullValue()));
        assertThat(part1.headers.get("Content-Type"),
                hasItems("text/xml; charset=UTF-8"));
        assertThat(part1.headers.get("Content-Transfer-Encoding"),
                hasItems("binary"));
        assertThat(part1.headers.get("Content-Id"), hasItems("part1"));
        assertThat(part1.headers.get("Content-Description"),
                hasItems("this is part1"));
        assertThat(part1.content, is(notNullValue()));
        assertThat(new String(part1.content), is(equalTo("<foo>bar</foo>")));

        MIMEPart part2 = parts.get(1);
        assertThat(part2.headers, is(notNullValue()));
        assertThat(part2.headers.get("Content-Type"), hasItems("image/jpeg"));
        assertThat(part2.headers.get("Content-Transfer-Encoding"),
                hasItems("binary"));
        assertThat(part2.headers.get("Content-Id"), hasItems("part2"));
        assertThat(part2.content, is(notNullValue()));
        assertThat(part2.content[0], is(equalTo((byte) 0xff)));
        assertThat(part2.content[1], is(equalTo((byte) 0xd8)));
    }

    @Test
    public void testEmptyPart() throws IOException {
        String boundary = "----=_Part_7_10584188.1123489648993";
        final byte[] chunk1 = ("--" + boundary + "\n"
                + "Content-Type: text/xml; charset=utf-8\n"
                + "Content-Id: part1\n"
                + "\n"
                + "--" + boundary + "\n"
                + "Content-Type: text/xml\n"
                + "Content-Id: part2\n"
                + "\n"
                + "<foo>bar</foo>\n"
                + "--" + boundary + "--").getBytes();

        List<MIMEPart> parts = parseParts(boundary, chunk1);
        assertThat(parts.size(), is(equalTo(2)));

        MIMEPart part1 = parts.get(0);
        assertThat(part1.headers, is(notNullValue()));
        assertThat(part1.headers.get("Content-Type"),
                hasItems("text/xml; charset=utf-8"));
        assertThat(part1.headers.get("Content-Id"), hasItems("part1"));
        assertThat(part1.content, is(notNullValue()));
        assertThat(part1.content.length, is(equalTo(0)));

        MIMEPart part2 = parts.get(1);
        assertThat(part2.headers.get("Content-Type"), hasItems("text/xml"));
        assertThat(part2.headers.get("Content-Id"), hasItems("part2"));
        assertThat(part1.content, is(notNullValue()));
        assertThat(new String(part2.content), is(equalTo("<foo>bar</foo>")));
    }

    @Test
    public void testNoHeaders() throws IOException {
        String boundary = "----=_Part_7_10584188.1123489648993";
        final byte[] chunk1 = ("--" + boundary + "\n"
                + "\n"
                + "<foo>bar</foo>\n"
                + "--" + boundary + "\n"
                + "\n"
                + "<bar>foo</bar>\n"
                + "--" + boundary + "--").getBytes();

        List<MIMEPart> parts = parseParts(boundary, chunk1);
        assertThat(parts.size(), is(equalTo(2)));

        MIMEPart part1 = parts.get(0);
        assertThat(part1.headers, is(notNullValue()));
        assertThat(part1.headers.size(), is(equalTo(0)));
        assertThat(part1.content, is(notNullValue()));
        assertThat(new String(part1.content), is(equalTo("<foo>bar</foo>")));

        MIMEPart part2 = parts.get(1);
        assertThat(part2.headers, is(notNullValue()));
        assertThat(part2.headers.size(), is(equalTo(0)));
        assertThat(part2.content, is(notNullValue()));
        assertThat(new String(part2.content), is(equalTo("<bar>foo</bar>")));
    }

    @Test
    public void testNoClosingBoundary() throws IOException {
        String boundary = "----=_Part_4_910054940.1065629194743";
        final byte[] chunk1 = concat(("--" + boundary + "\n"
                + "Content-Type: text/xml; charset=UTF-8\n"
                + "Content-Id: part1\n"
                + "\n"
                + "<foo>bar</foo>\n"
                + "--" + boundary + "\n"
                + "Content-Type: image/jpeg\n"
                + "Content-Transfer-Encoding: binary\n"
                + "Content-Id: part2\n"
                + "\n").getBytes(),
                new byte[] { (byte)0xff, (byte)0xd8 });

        boolean gotException = false;
        try {
            parseParts(boundary, chunk1);
        } catch (MIMEParsingException ex) {
            gotException = true;
            String msg = ex.getMessage();
            assertThat(msg, is(equalTo("No closing MIME boundary")));
        }
        assertThat(gotException, is(equalTo(true)));
    }

    @Test
    public void testInvalidClosingBoundary() throws IOException {
        String boundary = "----=_Part_4_910054940.1065629194743";
        final byte[] chunk1 = concat(("--" + boundary + "\n"
                + "Content-Type: text/xml; charset=UTF-8\n"
                + "Content-Id: part1\n"
                + "\n"
                + "<foo>bar</foo>\n"
                + "--" + boundary + "\n"
                + "Content-Type: image/jpeg\n"
                + "Content-Transfer-Encoding: binary\n"
                + "Content-Id: part2\n"
                + "\n").getBytes(),
                new byte[] { (byte)0xff, (byte)0xd8 },
                ("\n--" + boundary).getBytes());

        boolean gotException = false;
        try {
            parseParts(boundary, chunk1);
        } catch (MIMEParsingException ex) {
            gotException = true;
            String msg = ex.getMessage();
            assertThat(msg, is(equalTo("No closing MIME boundary")));
        }
        assertThat(gotException, is(equalTo(true)));
    }

    /**
     * Concatenate the specified byte arrays.
     * @param arrays byte arrays to concatenate
     * @return resulting array of the concatenation
     */
    private static byte[] concat(byte[] ... arrays){
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] res = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, res, pos, array.length);
            pos += array.length;
        }
        return res;
    }

    /**
     * Parse the parts in the given request chunks.
     * @param boundary boundary string
     * @param requestChunks request chunks
     * @return list of parts
     */
    static List<MIMEPart> parseParts(String boundary,
            byte[] ... requestChunks) {

        List<MIMEPart> parts = new LinkedList<>();
        MIMEParser parser = new MIMEParser(boundary);
        Iterator<MIMEEvent> it = parser.iterator();
        Map<String, List<String>> partHeaders = new HashMap<>();
        ByteBuffer partContent = null;
        for (byte[] chunk : requestChunks) {
            parser.offer(ByteBuffer.wrap(chunk));
            while (it.hasNext()) {
                MIMEEvent event = it.next();
                switch (event.getEventType()) {
                    case START_PART:
                        partHeaders = new HashMap<>();
                        partContent = null;
                        break;

                    case HEADER:
                        String name = ((MIMEEvent.Header) event).getName();
                        String value = ((MIMEEvent.Header) event).getValue();
                        assertThat(name, notNullValue());
                        assertThat(name.length(), not(equalTo(0)));
                        assertThat(value, notNullValue());
                        List<String> values = partHeaders.get(name);
                        if (values == null) {
                            values = new ArrayList<>();
                            partHeaders.put(name, values);
                        }
                        values.add(value);
                        break;

                    case CONTENT:
                        partContent = ((MIMEEvent.Content) event).getData();
                        assertThat(partContent, notNullValue());
                        break;

                    case END_PART:
                        parts.add(new MIMEPart(partHeaders, partContent));
                        break;
                }
            }
        }
        parser.close();
        return parts;
    }

    /**
     * Pair of part headers and body part content.
     */
    static final class MIMEPart {

        final Map<String, List<String>> headers;
        final byte[] content;

        MIMEPart(Map<String, List<String>> headers, ByteBuffer content) {
            this.headers = headers;
            this.content = Utils.toByteArray(content);
        }
    }
}
