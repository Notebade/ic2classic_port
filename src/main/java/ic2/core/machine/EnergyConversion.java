package ic2.core.machine;

final class EnergyConversion {
    static final int FE_PER_EU = 4;
    static int acceptedEu(int storedFe, int capacityFe, int offeredEu) {
        return Math.max(0, Math.min(offeredEu, (capacityFe - storedFe) / FE_PER_EU));
    }
    static int buffer(int packet) { return Math.clamp(packet, 1, 32768) * 32; }
}
