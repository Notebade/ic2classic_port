package ic2.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Radiation {
    private static final int REACTOR_RADIUS = 3;
    private static final int FALLOUT_RADIUS = 30;
    private static final int TICKS_PER_BLOCK = 80;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "radiation");
    private static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, IC2.MOD_ID);
    private static final DeferredHolder<MobEffect, RadiationEffect> EFFECT = EFFECTS.register("radiation", RadiationEffect::new);

    private Radiation() {}

    static void register(IEventBus bus) {
        EFFECTS.register(bus);
        NeoForge.EVENT_BUS.addListener(Radiation::preventProtectedDamage);
    }

    public static void exposeNearReactor(Level level, BlockPos pos, float containment) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(REACTOR_RADIUS))) {
            int damage = level.getRandom().nextInt(4);
            if (damage > 0) {
                hurt(entity, damage * containment);
            }
        }
    }

    public static void exposeToFallout(Level level, Vec3 origin) {
        var area = AABB.ofSize(origin, FALLOUT_RADIUS * 2, FALLOUT_RADIUS * 2, FALLOUT_RADIUS * 2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double distance = Math.sqrt(entity.distanceToSqr(origin));
            int duration = (int) (TICKS_PER_BLOCK * (FALLOUT_RADIUS - distance));
            if (duration > 0 && !Hazmat.protects(entity)) {
                entity.addEffect(new MobEffectInstance(EFFECT, duration));
            }
        }
    }

    private static void preventProtectedDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DAMAGE_TYPE) && Hazmat.protects(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void hurt(LivingEntity entity, float damage) {
        entity.hurt(entity.damageSources().source(DAMAGE_TYPE), damage);
    }

    private static final class RadiationEffect extends MobEffect {
        private RadiationEffect() {
            super(MobEffectCategory.HARMFUL, 0x4E9331);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int remaining, int amplifier) {
            return amplifier >= 4 || remaining % (25 >> amplifier) == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            hurt(entity, amplifier + 1);
            return true;
        }
    }
}
