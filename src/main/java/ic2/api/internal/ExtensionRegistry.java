package ic2.api.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Implementation detail, not part of the supported addon API. */
public final class ExtensionRegistry<K, V> {
    private final Map<K, V> entries = new LinkedHashMap<>();
    private boolean frozen;
    public synchronized void register(K id, V value) {
        if (frozen) throw new IllegalStateException("Addon registration has closed");
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(value, "value");
        if (entries.containsKey(id)) throw new IllegalArgumentException("Duplicate addon entry: " + id);
        entries.put(id, value);
    }
    public synchronized List<V> values() { return List.copyOf(entries.values()); }
    public synchronized void freeze() { frozen = true; }
}
