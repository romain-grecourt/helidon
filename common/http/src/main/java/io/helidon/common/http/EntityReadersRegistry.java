package io.helidon.common.http;

/**
 * Entity readers registry.
 */
public interface EntityReadersRegistry {

    void registerReader(EntityReader<?> reader);

    void registerStreamReader(EntityStreamReader<?> streamReader);
}
