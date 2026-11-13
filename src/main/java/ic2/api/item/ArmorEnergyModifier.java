package ic2.api.item;

/** Additive armor storage, voltage tier (32 EU = tier 1), transfer and external-provider flag. */
public record ArmorEnergyModifier(int capacity, int tier, int transferRate, boolean provider) {
    public static final ArmorEnergyModifier NONE = new ArmorEnergyModifier(0, 0, 0, false);
    public ArmorEnergyModifier {
        if (capacity < 0 || tier < 0 || transferRate < 0) throw new IllegalArgumentException("Negative armor energy modifier");
    }
}
