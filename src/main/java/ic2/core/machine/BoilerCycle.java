package ic2.core.machine;

final class BoilerCycle {
    record Steam(int water, int steam, int remainder) {}
    static Steam evaporate(int heat, int water, int steam, int remainder) {
        if (heat <= 3000 || water < 8 || steam > 128000 - 1280) return new Steam(water, steam, remainder);
        int numerator = remainder + (int) (heat / 3.75);
        int used = numerator / 1000;
        return new Steam(water - used, steam + used * 160, numerator % 1000);
    }
}
