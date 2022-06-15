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
package io.helidon.media.common;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.common.http.FormParams;

/**
 * {@link FormParams} helper.
 */
final class FormSupport {

    private static final Pattern URL_ENCODED = pattern("&");
    private static final Pattern TEXT_PLAIN = pattern("\n");

    private FormSupport() {
    }

    /**
     * Convert the given form params instance to URL encoded form data.
     *
     * @param formParams form params
     * @param charset    charset
     * @return data
     */
    static String writeURLEncoded(FormParams formParams, Charset charset) {
        return write(formParams, '&', s -> URLEncoder.encode(s, charset));
    }

    /**
     * Convert the given form params instance to text plain form data.
     *
     * @param formParams form params
     * @return data
     */
    static String writeTextPlain(FormParams formParams) {
        return write(formParams, '\n', Function.identity());
    }

    /**
     * Read the given data as URL encoded form data.
     *
     * @param data    data
     * @param charset charset
     * @return form params
     */
    static FormParams readURLEncoded(String data, Charset charset) {
        return read(data, URL_ENCODED, s -> URLDecoder.decode(s, charset));
    }

    /**
     * Read the given data as text plain form data.
     *
     * @param data data
     * @return form params
     */
    static FormParams readTextPlain(String data) {
        return read(data, TEXT_PLAIN, Function.identity());
    }

    private static Pattern pattern(String sep) {
        return Pattern.compile(String.format("([^=%1$s]+)=?([^%1$s]+)?%1$s?", sep));
    }

    private static FormParams read(String data, Pattern pattern, Function<String, String> decoder) {
        FormParams.Builder builder = FormParams.builder();
        Matcher m = pattern.matcher(data);
        while (m.find()) {
            final String key = m.group(1);
            final String value = m.group(2);
            if (value == null) {
                builder.add(decoder.apply(key));
            } else {
                builder.add(decoder.apply(key), decoder.apply(value));
            }
        }
        return builder.build();
    }

    private static String write(FormParams formParams, char sep, Function<String, String> encoder) {
        StringBuilder result = new StringBuilder();
        formParams.toMap().forEach((key, values) -> {
            if (values.size() == 0) {
                if (result.length() > 0) {
                    result.append(sep);
                }
                result.append(encoder.apply(key));
            } else {
                for (String value : values) {
                    if (result.length() > 0) {
                        result.append(sep);
                    }
                    result.append(encoder.apply(key));
                    result.append("=");
                    result.append(encoder.apply(value));
                }
            }
        });
        return result.toString();
    }
}
