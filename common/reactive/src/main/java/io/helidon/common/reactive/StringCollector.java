package io.helidon.common.reactive;

/**
 * {@link Collector} implementation that concatenates items
 * {@link Object#toString()} in a {@link String}.
 *
 * @param <T> collected item type
 */
final class StringCollector<T extends Object> implements Collector<String, T> {

    private final StringBuilder sb;

    StringCollector() {
        this.sb = new StringBuilder();
    }

    @Override
    public void collect(T item) {
        sb.append(item.toString());
    }

    @Override
    public String value() {
        return sb.toString();
    }
}
