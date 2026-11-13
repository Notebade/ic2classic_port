package ic2.api.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Additional protection is evaluated before vanilla physical armor. Queries must not mutate equipment. */
public interface ProtectiveArmor {
    record Absorption(int priority, double ratio, int maximum) {
        public static final Absorption NONE = new Absorption(0, 0, 0);
        public Absorption {
            if (!Double.isFinite(ratio) || ratio < 0 || maximum < 0) throw new IllegalArgumentException("Invalid armor absorption");
        }
    }
    enum Wear { ABSORBED, VANILLA }
    Absorption absorption(ItemStack stack, LivingEntity wearer, DamageSource source, double damage, EquipmentSlot slot);
    void absorbed(ItemStack stack, LivingEntity wearer, DamageSource source, int damage, EquipmentSlot slot, Wear kind);
}
