package ic2.core.machine;

final class SolarTurbineCycle {
    static final int MAX_HEAT = 24000;
    static final int MAX_WATER = 40000;
    static final int NOMINAL_BUFFER = 100;
    static final int MAX_PACKET = 32;
    static final int MAX_STORED = NOMINAL_BUFFER + MAX_PACKET - 1;
    static final int ENERGY_DENOMINATOR = 30000000;

    private SolarTurbineCycle() {}

    record State(int heat, int water, int waterRemainder, int energyRemainder) {
        State {
            if (heat < 0 || heat > MAX_HEAT || water < 0 || water > MAX_WATER
                    || waterRemainder < 0 || waterRemainder >= 6000
                    || energyRemainder < 0 || energyRemainder >= ENERGY_DENOMINATOR) {
                throw new IllegalArgumentException("Invalid solar turbine state");
            }
        }

        Fill fill(int incoming) {
            int accepted = Math.min(Math.max(0, incoming), (MAX_WATER - water) / 2);
            return new Fill(new State(heat, water + 2 * accepted, waterRemainder, energyRemainder), accepted);
        }

        Step step(int energy, boolean sunny) {
            return step(energy, sunny, 1);
        }

        Step step(int energy, boolean sunny, double multiplier) {
            int nextHeat = sunny ? Math.min(MAX_HEAT, heat + 2) : Math.max(0, heat - 1);
            if (water == 0 || nextHeat == 0) return new Step(new State(nextHeat, water, waterRemainder, energyRemainder), energy, 0);
            int consumed = waterRemainder + nextHeat;
            int waterLeft = Math.max(0, water - consumed / 6000);
            int remainder = energyRemainder;
            if (energy < NOMINAL_BUFFER) {
                long generated = remainder + (long) (water * (double) nextHeat * multiplier);
                energy += (int) (generated / ENERGY_DENOMINATOR);
                remainder = (int) (generated % ENERGY_DENOMINATOR);
            }
            int outputHundredths = (int) (water * (double) nextHeat * multiplier * 100 / ENERGY_DENOMINATOR);
            return new Step(new State(nextHeat, waterLeft, consumed % 6000, remainder), energy, outputHundredths);
        }
    }

    record Fill(State next, int accepted) {}
    record Step(State next, int energy, int outputHundredths) {}
}
