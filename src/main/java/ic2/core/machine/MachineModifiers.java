package ic2.core.machine;

import ic2.api.machine.MachineUpgrade;
import net.neoforged.neoforge.items.IItemHandler;

record MachineModifiers(int capacity, int packet, double speed, double demand) {
    static MachineModifiers read(IItemHandler inventory, int firstSlot, int baseCapacity, int baseTier) {
        long extraStorage = 0, extraTier = 0;
        double storage = 1, speed = 1, demand = 1;
        for (int slot = firstSlot; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof MachineUpgrade upgrade)) continue;
            int count = stack.getCount();
            extraStorage += (long) Math.max(0, upgrade.extraEnergyStorage(stack)) * count;
            extraTier += (long) Math.max(0, upgrade.extraTier(stack)) * count;
            storage *= Math.pow(positive(upgrade.storageMultiplier(stack)), count);
            speed *= Math.pow(positive(upgrade.speedMultiplier(stack)), count);
            demand *= Math.pow(positive(upgrade.energyDemandMultiplier(stack)), count);
        }
        int base = (int) Math.min(Integer.MAX_VALUE, baseCapacity + extraStorage);
        return new MachineModifiers(MachineUpgradeRules.scale(base, storage),
            MachineUpgradeRules.packet(baseTier, (int) Math.min(13, extraTier)), speed, demand);
    }
    private static double positive(double value) { return Double.isNaN(value) || value < 0 ? 1 : value; }
}
