package ic2.core.machine;

final class ChargingDistribution {
    private ChargingDistribution() {}
    static int[] split(int total, int[] weights, boolean[] ready) {
        if (weights.length != 6 || ready.length != 6 || total < 0) throw new IllegalArgumentException("Invalid redirector split");
        long sum = 0;
        for (int side = 0; side < 6; side++) {
            if (weights[side] < 0 || weights[side] > 100) throw new IllegalArgumentException("Invalid share");
            if (ready[side]) sum += weights[side];
        }
        int[] result = new int[6]; int remaining = total;
        if (sum == 0) return result;
        for (int side = 0; side < 6; side++) if (ready[side] && weights[side] > 0) {
            int amount = (int) Math.min(remaining, (total * (long) weights[side] + sum - 1) / sum);
            result[side] = amount; remaining -= amount;
        }
        return result;
    }
    static int transfer(int offered, int stored, int capacity, int rate) {
        return Math.max(0, Math.min(offered, Math.min(capacity - stored, rate)));
    }
    static int share(int energy, int remainingItems) { return remainingItems <= 0 ? 0 : Math.max(0, energy) / remainingItems; }
}
