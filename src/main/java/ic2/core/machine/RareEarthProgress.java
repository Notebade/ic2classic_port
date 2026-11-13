package ic2.core.machine;

/** Exact 1/64 material units; one output batch contains two dusts. */
record RareEarthProgress(String material, int units) {
    static final int UNITS_PER_OUTPUT = 64000;

    RareEarthProgress {
        if (material == null || units < 0 || units >= UNITS_PER_OUTPUT) {
            throw new IllegalArgumentException("Invalid rare-earth progress");
        }
    }

    RareEarthProgress select(String material) {
        return this.material.equals(material) ? this : new RareEarthProgress(material, 0);
    }

    Batch add(int yield) {
        if (yield < 0 || yield > UNITS_PER_OUTPUT) throw new IllegalArgumentException("Invalid material yield");
        int total = units + yield;
        return new Batch(new RareEarthProgress(material, total % UNITS_PER_OUTPUT), total / UNITS_PER_OUTPUT);
    }

    record Batch(RareEarthProgress next, int batches) {}
}
