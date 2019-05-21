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

/**
 * The {@code MIMEParsingException} class is the base
 * exception class for all MIME message parsing exceptions.
 *
 */

public class MIMEParsingException extends java.lang.RuntimeException {

    /**
     * Constructs a new exception with {@code null} as its
     * detail message. The cause is not initialized.
     */
    public MIMEParsingException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail
     * message.  The cause is not initialized.
     *
     * @param message The detail message which is later
     *                retrieved using the getMessage method
     */
    public MIMEParsingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail
     * message and cause.
     *
     * @param message The detail message which is later retrieved
     *                using the getMessage method
     * @param cause   The cause which is saved for the later
     *                retrieval throw by the getCause method
     */
    public MIMEParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new WebServiceException with the specified cause
     * and a detail message of <tt>(cause==null ? null :
     * cause.toString())</tt> (which typically contains the
     * class and detail message of <tt>cause</tt>).
     *
     * @param cause The cause which is saved for the later
     *              retrieval throw by the getCause method.
     *              (A <tt>null</tt> value is permitted, and
     *              indicates that the cause is nonexistent or
     *              unknown.)
     */
    public MIMEParsingException(Throwable cause) {
        super(cause);
    }
}
