package ic2.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Original one-shot LV hotbar recharge: unused remainder is discarded only after a real transfer. */
public final class DisposableBatteryItem extends Item {
    public DisposableBatteryItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int remaining = 1000;
            for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                var target = player.getInventory().getItem(slot);
                if (!RechargeableItem.supports(target, 32)) continue;
                var item = (ic2.api.item.ElectricItem) target.getItem();
                int amount = Math.min(remaining, item.capacity(target) - item.charge(target));
                if (amount > 0) { item.setCharge(target, item.charge(target) + amount); remaining -= amount; }
            }
            if (remaining < 1000) { stack.shrink(1); player.getInventory().setChanged(); }
        }
        return InteractionResultHolder.success(stack);
    }
}
