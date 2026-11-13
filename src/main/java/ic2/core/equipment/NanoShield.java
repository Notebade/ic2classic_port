package ic2.core.equipment;

import ic2.api.item.ElectricItem;
import ic2.core.item.EuCharge;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

final class NanoShield extends ShieldItem implements ElectricItem {
    NanoShield(Properties properties) { super(properties.durability(1)); }
    @Override public int capacity() { return 100000; }
    @Override public int transferRate() { return 300; }
    @Override public int minimumPacket() { return 128; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public int charge(ItemStack stack) { return EuCharge.get(stack, capacity()); }
    @Override public void setCharge(ItemStack stack, int amount) { EuCharge.set(stack, capacity(), amount); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return !ic2.api.item.ElectricEquipment.canUse(player.getItemInHand(hand), 50) ? InteractionResultHolder.fail(player.getItemInHand(hand)) : super.use(level, player, hand);
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!ic2.api.item.ElectricEquipment.canUse(stack, 50)) entity.stopUsingItem();
    }
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack, int damage, T entity, Consumer<Item> broken) {
        int cost = ic2.api.item.ElectricEquipment.energyCost(stack, (int) Math.min(Integer.MAX_VALUE, 50L * Math.max(0, damage)));
        setCharge(stack, Math.max(0, charge(stack) - cost));
        return 0;
    }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * charge(stack) / capacity()); }
    @Override public int getBarColor(ItemStack stack) { return 0x42CDEA; }
}
