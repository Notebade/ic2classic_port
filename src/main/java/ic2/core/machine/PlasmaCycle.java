package ic2.core.machine;

final class PlasmaCycle {
    static final int CAPACITY = 5_120_000;
    record State(int energy, int reserve, int plasma) {
        State supplyMatter() { return reserve < 50 ? new State(energy, reserve + 100, plasma) : this; }
        State step() {
            return energy >= 10240 && reserve > 0 && plasma < 10000
                ? new State(energy - 10240, reserve - 1, plasma + 1) : this;
        }
    }
}
