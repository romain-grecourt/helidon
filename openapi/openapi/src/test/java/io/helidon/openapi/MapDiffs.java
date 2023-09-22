package io.helidon.openapi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Test utility to compute map differences.
 */
public final class MapDiffs {

    private MapDiffs() {
    }

    /**
     * Compute differences between two maps.
     *
     * @param left  left map
     * @param right right map
     * @return list of differences
     */
    static List<Diff> diffs(Map<?, ?> left, Map<?, ?> right) {
        List<Diff> diffs = new ArrayList<>();
        Iterator<Map.Entry<String, String>> leftEntries = flatten(left, "").iterator();
        Iterator<Map.Entry<String, String>> rightEntries = flatten(right, "").iterator();
        while (true) {
            boolean hasLeft = leftEntries.hasNext();
            boolean hasRight = rightEntries.hasNext();
            if (hasLeft && hasRight) {
                Map.Entry<String, String> leftEntry = leftEntries.next();
                Map.Entry<String, String> rightEntry = rightEntries.next();
                if (!leftEntry.equals(rightEntry)) {
                    diffs.add(new Diff(leftEntry, rightEntry));
                }
            } else if (hasLeft) {
                diffs.add(new Diff(leftEntries.next(), null));
            } else if (hasRight) {
                diffs.add(new Diff(null, rightEntries.next()));
            } else {
                return diffs;
            }
        }
    }

    private static List<Map.Entry<String, String>> flatten(Map<?, ?> map, String prefix) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> node) {
                result.addAll(flatten(node, entry.getKey() + "."));
            } else {
                result.add(Map.entry(prefix + entry.getKey(), entry.getValue().toString()));
            }
        }
        return result;
    }

    /**
     * A difference between two map entries.
     *
     * @param left  left entry
     * @param right right entry
     */
    public record Diff(Map.Entry<String, String> left, Map.Entry<String, String> right) {

        @Override
        public String toString() {
            if (left == null && right != null) {
                return "ADDED   >> " + right;
            }
            if (left != null && right == null) {
                return "REMOVED << " + left;
            }
            if (left != null) {
                return "ADDED   >> " + left + System.lineSeparator() + "REMOVED << " + right;
            }
            return "?";
        }
    }
}
