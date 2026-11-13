package ic2.api.internal;

public final class ExtensionRegistryTest {
    public static void main(String[] args) {
        var registry = new ExtensionRegistry<String, String>();
        registry.register("addon:first", "first");
        var snapshot = registry.values();
        registry.register("addon:second", "second");
        require(snapshot.size() == 1 && registry.values().size() == 2, "snapshots do not change after registration");
        expect(UnsupportedOperationException.class, () -> snapshot.add("mutation"));
        expect(IllegalArgumentException.class, () -> registry.register("addon:first", "replacement"));
        require(registry.values().getFirst().equals("first"), "duplicate cannot overwrite existing entry");
        expect(NullPointerException.class, () -> registry.register(null, "invalid"));
        registry.freeze();
        registry.freeze();
        expect(IllegalStateException.class, () -> registry.register("addon:late", "late"));
        require(registry.values().size() == 2, "freeze preserves registrations");
        System.out.println("Addon registry lifecycle: all checks passed");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void expect(Class<? extends Exception> expected, Runnable action) {
        try { action.run(); } catch (Exception error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}
