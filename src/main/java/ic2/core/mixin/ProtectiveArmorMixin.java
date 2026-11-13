package ic2.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ic2.core.equipment.ArmorDefense;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class ProtectiveArmorMixin {
    @WrapMethod(method = "getDamageAfterArmorAbsorb")
    private float ic2$armor(DamageSource source, float damage, Operation<Float> original) {
        float remaining = ArmorDefense.absorb((LivingEntity) (Object) this, source, damage);
        return remaining <= 0 ? 0 : original.call(source, remaining);
    }
}
