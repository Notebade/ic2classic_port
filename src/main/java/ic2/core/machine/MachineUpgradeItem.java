package ic2.core.machine;

import ic2.api.machine.MachineUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

final class MachineUpgradeItem extends Item implements MachineUpgrade {
    enum Kind {
        OVERCLOCKER(1.3, 1.6), QUANTUM_OVERCLOCKER(1.6, 1.3), TRANSFORMER(1, 1),
        ENERGY_STORAGE(1, 1), EFFICIENCY(1, .95), ENERGY_STORAGE_MULTIPLIER(1, 1), SLOWNESS(.5, .6);
        final double speed, demand;
        Kind(double speed, double demand) { this.speed = speed; this.demand = demand; }
    }
    private final Kind kind;

    MachineUpgradeItem(Properties properties, Kind kind) { super(properties); this.kind = kind; }

    static void register(DeferredRegister.Items items) {
        for (var kind : Kind.values()) items.registerItem("upgrade_" + kind.name().toLowerCase(java.util.Locale.ROOT),
            p -> new MachineUpgradeItem(p.stacksTo(kind == Kind.ENERGY_STORAGE_MULTIPLIER ? 3 : kind == Kind.EFFICIENCY ? 16 : 64), kind));
    }

    @Override public double speedMultiplier(ItemStack stack) { return kind.speed; }
    @Override public double energyDemandMultiplier(ItemStack stack) { return kind.demand; }
    @Override public int extraEnergyStorage(ItemStack stack) { return kind == Kind.ENERGY_STORAGE ? 10000 : 0; }
    @Override public double storageMultiplier(ItemStack stack) { return kind == Kind.ENERGY_STORAGE_MULTIPLIER ? 2 : 1; }
    @Override public int extraTier(ItemStack stack) { return kind == Kind.TRANSFORMER ? 1 : 0; }
}
