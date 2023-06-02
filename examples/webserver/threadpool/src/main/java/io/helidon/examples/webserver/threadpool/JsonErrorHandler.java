package io.helidon.examples.webserver.threadpool;

import io.helidon.common.http.Http;
import io.helidon.nima.webserver.http.ErrorHandler;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import jakarta.json.JsonException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link JsonException} error handler.
 */
final class JsonErrorHandler implements ErrorHandler<JsonException> {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void handle(ServerRequest req, ServerResponse res, JsonException ex) {
        LOGGER.log(Level.FINE, "Invalid JSON", ex);
        res.status(Http.Status.BAD_REQUEST_400).send("Invalid JSON");
    }
}
