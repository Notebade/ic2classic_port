package ic2.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Hold use to charge one hotbar item per tick, then the offhand. */
public final class PortableBatteryItem extends RechargeableItem implements ic2.api.item.ArmorBattery {
    private final int armorRate;
    public PortableBatteryItem(Properties properties, int capacity, int transferRate, int packet) {
        this(properties, capacity, transferRate, packet, -1);
    }
    public PortableBatteryItem(Properties properties, int capacity, int transferRate, int packet, int armorRate) {
        super(properties, capacity, transferRate, packet);
        this.armorRate = armorRate;
    }
    @Override public int armorTransferRate(ItemStack stack) { return armorRate; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (charge(stack) == 0) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }
        int available = Math.min(charge(stack), 2 * transferRate());
        for (int slot = 0; slot < 9; slot++) {
            if (transfer(stack, player.getInventory().getItem(slot), available)) {
                player.getInventory().setChanged();
                return;
            }
        }
        if (player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && transfer(stack, player.getOffhandItem(), available)) {
            player.getInventory().setChanged();
            return;
        }
        player.stopUsingItem();
    }

    private boolean transfer(ItemStack source, ItemStack target, int available) {
        if (source == target || !supports(target, minimumPacket())) {
            return false;
        }
        ic2.api.item.ElectricItem cell = (ic2.api.item.ElectricItem) target.getItem();
        int stored = cell.charge(target);
        int moved = Math.min(available, cell.capacity(target) - stored);
        if (moved <= 0) {
            return false;
        }
        cell.setCharge(target, stored + moved);
        setCharge(source, charge(source) - moved);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() == newStack.getItem();
    }
}
