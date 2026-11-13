package ic2.api.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Source-compatible tool consumption and worn-provider charging. Mutations require the server thread. */
public final class ElectricEquipment {
    private ElectricEquipment() {}

    /** Efficiency adds 15% per level, then Unbreaking removes 10% per level; each step truncates. */
    public static int energyCost(ItemStack stack, int base) {
        if (base < 0) throw new IllegalArgumentException("Negative energy cost");
        int increased = (int) (base * (1D + .15D * level(stack, Enchantments.EFFICIENCY)));
        return (int) (increased * Math.max(0D, 1D - .1D * level(stack, Enchantments.UNBREAKING)));
    }
    private static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().stream()
            .filter(entry -> entry.getKey().is(key)).mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue).findFirst().orElse(0);
    }
    /** Pure check; never charges from the wearer or changes either inventory. */
    public static boolean canUse(ItemStack stack, int base) {
        return ElectricItem.supports(stack, Integer.MAX_VALUE, true) && ((ElectricItem) stack.getItem()).charge(stack) >= energyCost(stack, base);
    }
    /** Fills before/after use from worn providers. Failed use keeps existing tool energy. */
    public static boolean use(ItemStack stack, int base, LivingEntity wearer) {
        if (!ElectricItem.supports(stack, Integer.MAX_VALUE, true) || wearer != null && wearer.level().isClientSide) return false;
        int cost = energyCost(stack, base);
        chargeFromArmor(stack, wearer);
        var item = (ElectricItem) stack.getItem();
        if (item.charge(stack) < cost) return false;
        item.setCharge(stack, item.charge(stack) - cost);
        chargeFromArmor(stack, wearer);
        return true;
    }
    /** Source armor transfer bypasses charger packet/rate limits, but requires canProvideEnergy. */
    public static int chargeFromArmor(ItemStack target, LivingEntity wearer) {
        if (wearer == null || wearer.level().isClientSide || !ElectricItem.supports(target, Integer.MAX_VALUE, true)) return 0;
        var item = (ElectricItem) target.getItem();
        int needed = item.capacity(target) - item.charge(target), transferred = 0;
        for (var armor : wearer.getArmorSlots()) {
            if (needed <= 0) break;
            if (armor == target || !ElectricItem.supports(armor, Integer.MAX_VALUE, false)) continue;
            var source = (ElectricItem) armor.getItem();
            int amount = Math.min(needed, source.charge(armor));
            if (amount <= 0) continue;
            source.setCharge(armor, source.charge(armor) - amount);
            item.setCharge(target, item.charge(target) + amount);
            needed -= amount; transferred += amount;
        }
        if (transferred > 0 && wearer instanceof Player player) {
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        }
        return transferred;
    }
    /** Offers generated EU to worn stores in armor order, respecting each item's transfer rate. Returns accepted EU. */
    public static int offerToArmor(LivingEntity wearer, int offered) {
        if (offered < 0) throw new IllegalArgumentException("Negative energy offer");
        if (wearer.level().isClientSide) return 0;
        int remaining = offered;
        for (var stack : wearer.getArmorSlots()) {
            if (remaining <= 0) break;
            if (!ElectricItem.supports(stack, Integer.MAX_VALUE, true)) continue;
            var item = (ElectricItem) stack.getItem();
            int amount = Math.min(remaining, Math.min(item.transferRate(stack), item.capacity(stack) - item.charge(stack)));
            if (amount <= 0) continue;
            item.setCharge(stack, item.charge(stack) + amount); remaining -= amount;
        }
        if (remaining < offered && wearer instanceof Player player) player.getInventory().setChanged();
        return offered - remaining;
    }
}
