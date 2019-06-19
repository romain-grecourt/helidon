package io.helidon.common.http;

/**
 * Entity writers registry.
 */
public interface EntityWritersRegistry {

   EntityWritersRegistry registerWriter(EntityWriter<?> writer);

   EntityWritersRegistry registerStreamWriter(EntityStreamWriter<?> writer); 
}
