package io.helidon.webserver.examples.tutorial;

import io.helidon.common.GenericType;

import java.util.List;

/**
 * Represents a single comment.
 */
record Comment(User user, String message) {

    static final GenericType<List<Comment>> LIST_TYPE = new GenericType<>() {};

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (user != null) {
            result.append(user.alias());
        }
        result.append(": ");
        result.append(message);
        return result.toString();
    }
}
