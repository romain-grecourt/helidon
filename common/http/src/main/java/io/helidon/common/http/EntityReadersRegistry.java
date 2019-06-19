package io.helidon.common.http;

/**
 * Entity readers registry.
 */
public interface EntityReadersRegistry {

    EntityReadersRegistry registerReader(EntityReader<?> reader);

    EntityReadersRegistry registerStreamReader(EntityStreamReader<?> streamReader);
}
