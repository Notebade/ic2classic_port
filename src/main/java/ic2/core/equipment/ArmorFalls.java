package ic2.core.equipment;

import ic2.api.item.FallProtectiveArmor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

final class ArmorFalls {
    private ArmorFalls() {}
    static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        var stack = player.getItemBySlot(EquipmentSlot.FEET);
        if (stack.getCount() != 1 || !(stack.getItem() instanceof FallProtectiveArmor item)) return;
        double multiplier = event.getDamageMultiplier() * player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        double safeDistance = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        if (!Double.isFinite(multiplier) || multiplier <= 0) return;
        int damage = (int) Math.min(Integer.MAX_VALUE, Math.max(0, Math.ceil((event.getDistance() - safeDistance) * multiplier)));
        if (damage == 0) return;
        int absorbed = Math.clamp(item.absorbFall(stack, player, damage, false), 0, damage);
        if (absorbed == damage) event.setCanceled(true);
        else if (absorbed > 0) event.setDistance(Math.nextDown((float) (safeDistance + (damage - absorbed) / multiplier)));
    }
}
