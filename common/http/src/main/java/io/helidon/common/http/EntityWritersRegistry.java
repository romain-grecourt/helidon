package io.helidon.common.http;

/**
 * Entity writers registry.
 */
public interface EntityWritersRegistry {

   void registerWriter(EntityWriter<?> writer);

   void registerStreamWriter(EntityStreamWriter<?> writer); 
}
