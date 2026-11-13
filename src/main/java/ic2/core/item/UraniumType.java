package ic2.core.item;

import java.util.Locale;

public enum UraniumType {
    STANDARD(10_000, 1, 1, 20, 100, 2),
    BLAZE(10_000, 1, 1, 20, 400, 6),
    CHARCOAL(20_000, 1, 1, 12, 60, 2),
    ENDER_PEARL(5_000, 1, 1, 40, 25, 2),
    NETHER_STAR(20_000, 2, 3, 100, 120, 2),
    REDSTONE(8_000, 2, 1, 20, 50, 2);

    private final int durability;
    private final int pulses;
    private final int connectionPulses;
    private final int energyPerPulse;
    private final int heatPercent;
    private final int explosionPower;

    UraniumType(int durability, int pulses, int connectionPulses, int energyPerPulse, int heatPercent, int explosionPower) {
        this.durability = durability;
        this.pulses = pulses;
        this.connectionPulses = connectionPulses;
        this.energyPerPulse = energyPerPulse;
        this.heatPercent = heatPercent;
        this.explosionPower = explosionPower;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int durability() {
        return durability;
    }

    public int pulses() {
        return pulses;
    }

    public int connectionPulses() {
        return connectionPulses;
    }

    public int energyPerPulse() {
        return energyPerPulse;
    }

    public int heatPerCell(int pulseCount) {
        return pulseCount * (pulseCount + 1) * 2 * heatPercent / 100;
    }

    public int explosionPower() {
        return explosionPower;
    }

    public int fusionHeat() {
        return switch (this) { case STANDARD -> 5; case BLAZE -> 25; case CHARCOAL -> 1;
            case ENDER_PEARL -> 3; case NETHER_STAR -> 20; case REDSTONE -> 10; };
    }
}
