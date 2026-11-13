package ic2.core.machine;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

final class WindRotor extends Item {
    enum Kind {
        WOODEN("wooden", 8, 1, 11250), WOOL("wool", 8, 1.5, 5675), IRON("iron", 10, 1, 136875),
        CARBON("carbon", 8, 1, 68437), ALLOY("mixed_alloy", 10, 1, 273750), IRIDIUM("iridium", 10, 1, 0);
        final String id; final int radius, durability; final double effectiveness;
        Kind(String id, int radius, double effectiveness, int durability) {
            this.id = id; this.radius = radius; this.effectiveness = effectiveness; this.durability = durability;
        }
    }
    final Kind kind;
    WindRotor(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    static void register(DeferredRegister.Items items) {
        for (Kind kind : Kind.values()) items.registerItem(kind.id + "_rotor", p -> new WindRotor(p, kind),
            kind.durability == 0 ? new Properties().stacksTo(1) : new Properties().durability(kind.durability).setNoRepair());
    }
}
