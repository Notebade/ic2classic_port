package ic2.core.item;

import ic2.core.IC2;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Shared native storage, independent of the item's class hierarchy. */
public final class EuCharge {
    private EuCharge() {}
    public static int get(ItemStack stack, int capacity) { return Mth.clamp(stack.getOrDefault(IC2.BATTERY_CHARGE.get(), 0), 0, capacity); }
    public static void set(ItemStack stack, int capacity, int amount) {
        int charge = Mth.clamp(amount, 0, capacity);
        if (charge == 0) stack.remove(IC2.BATTERY_CHARGE.get()); else stack.set(IC2.BATTERY_CHARGE.get(), charge);
    }
}
