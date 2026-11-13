package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** Item-owned EU storage for native chargers, battery slots and portable charging. */
public interface ElectricItem {
    int capacity();
    int transferRate();
    int minimumPacket();
    int charge(ItemStack stack);
    void setCharge(ItemStack stack, int charge);
    default boolean canProvideEnergy() { return true; }
    /** Per-stack variants for modular equipment; defaults preserve fixed-storage addon compatibility. */
    default int capacity(ItemStack stack) { return capacity(); }
    default int transferRate(ItemStack stack) { return transferRate(); }
    default int minimumPacket(ItemStack stack) { return minimumPacket(); }
    default boolean canProvideEnergy(ItemStack stack) { return canProvideEnergy(); }
    static boolean supports(ItemStack stack, int machinePacket, boolean charging) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof ElectricItem item)) return false;
        int capacity = item.capacity(stack), charge = item.charge(stack);
        return capacity > 0 && charge >= 0 && charge <= capacity && item.transferRate(stack) >= 0
            && item.minimumPacket(stack) > 0 && item.minimumPacket(stack) <= machinePacket && (charging || item.canProvideEnergy(stack));
    }
}
