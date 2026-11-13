package ic2.core.machine;

record AlloyInputAmounts(int first, int second) {
    AlloyInputAmounts {
        if (first < 1 || second < 1) throw new IllegalArgumentException("Alloy ingredients require positive counts");
    }

    boolean fits(int firstAvailable, int secondAvailable) { return firstAvailable >= first && secondAvailable >= second; }
    AlloyInputAmounts reversed() { return new AlloyInputAmounts(second, first); }
}
