package ic2.core.machine;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

final class ElectricHazards {
    private static final ResourceKey<DamageType> ELECTRICITY = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("ic2", "electricity"));

    private ElectricHazards() {}

    static DamageSource source(Level level) { return level.damageSources().source(ELECTRICITY); }
    static DamageSource source(Level level, net.minecraft.world.entity.Entity owner) { return level.damageSources().source(ELECTRICITY, owner); }

    static boolean shockable(net.minecraft.world.entity.LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !ic2.core.Hazmat.protects(entity)
            && !(entity instanceof net.minecraft.world.entity.player.Player player && player.isCreative());
    }
}
