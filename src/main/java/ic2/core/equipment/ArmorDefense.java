package ic2.core.equipment;

import ic2.api.item.ProtectiveArmor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class ArmorDefense {
    private record Part(ItemStack stack, ProtectiveArmor item, EquipmentSlot slot, ProtectiveArmor.Absorption protection) {}
    private ArmorDefense() {}
    public static float absorb(LivingEntity wearer, DamageSource source, float damage) {
        var parts = new ArrayList<Part>();
        var totals = new HashMap<Integer, Double>();
        for (var slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            var stack = wearer.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ProtectiveArmor item)) continue;
            var protection = item.absorption(stack, wearer, source, damage, slot);
            parts.add(new Part(stack, item, slot, protection));
            totals.merge(protection.priority(), protection.ratio(), Double::sum);
        }
        parts.sort(Comparator.<Part>comparingInt(p -> p.protection.priority()).reversed()
            .thenComparingDouble(p -> p.protection.ratio() == 0 ? Double.MAX_VALUE : p.protection.maximum() / p.protection.ratio()));
        double remaining = damage;
        for (var part : parts) {
            var p = part.protection;
            double absorbed = Math.min(remaining, Math.min(p.maximum(), damage * p.ratio() / Math.max(1, totals.get(p.priority()))));
            if (absorbed > 0) part.item.absorbed(part.stack, wearer, source, Math.max(1, (int) absorbed), part.slot, ProtectiveArmor.Wear.ABSORBED);
            remaining -= absorbed;
        }
        if (remaining < .000001) return 0;
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && (wearer.getArmorValue() > 0 || wearer.getAttributeValue(Attributes.ARMOR_TOUGHNESS) > 0)) {
            for (var part : parts) part.item.absorbed(part.stack, wearer, source, Math.max(1, (int) (remaining / 4)), part.slot, ProtectiveArmor.Wear.VANILLA);
        }
        return (float) remaining;
    }
}
