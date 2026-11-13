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
    static boolean supports(ItemStack stack, int machinePacket, boolean charging) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof ElectricItem item)) return false;
        int capacity = item.capacity(), charge = item.charge(stack);
        return capacity > 0 && charge >= 0 && charge <= capacity && item.transferRate() >= 0
            && item.minimumPacket() > 0 && item.minimumPacket() <= machinePacket && (charging || item.canProvideEnergy());
    }
}
