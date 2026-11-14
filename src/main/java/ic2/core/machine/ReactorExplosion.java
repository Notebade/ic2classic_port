package ic2.core.machine;

import ic2.core.Radiation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * IC2's deterministic angular rays, low outer-shell drop rate and radiation effects.
 */
public final class ReactorExplosion {

    private record Hit(BlockState state, boolean drops) {}
    private final ServerLevel level;
    private final Vec3 origin;
    private final float power;
    private final Explosion event;
    private final AABB bounds;
    private final boolean electrical;
    private final boolean laser;
    private final float dropRate;
    private final Map<BlockPos, Hit> blocks = new LinkedHashMap<>();
    private final Map<Entity, Vec3> motion = new LinkedHashMap<>();
    private List<Entity> entities;

    private ReactorExplosion(ServerLevel level, BlockPos pos, float power, boolean electrical) {
        this(level, electrical ? Vec3.atCenterOf(pos) : Vec3.atLowerCornerOf(pos), power, electrical, null);
    }

    private ReactorExplosion(ServerLevel level, Vec3 origin, float power, boolean electrical, Entity shot) {
        this.level = level;
        this.laser = shot != null;
        this.dropRate = laser ? .85f : electrical ? .75f : .01f;
        this.electrical = electrical;
        this.origin = origin;
        this.power = power;
        event = new Explosion(level, shot, origin.x, origin.y, origin.z, (float)ReactorBlastMath.radius(power), false, Explosion.BlockInteraction.DESTROY);
        bounds = new AABB(BlockPos.containing(origin)).inflate(ReactorBlastMath.radius(power));
    }

    public static void detonate(ServerLevel level, BlockPos pos, float power) {
        if (Float.isFinite(power) && power > 0) new ReactorExplosion(level, pos, power, false).run();
    }
    static void detonateElectrical(ServerLevel level, BlockPos pos) { new ReactorExplosion(level, pos, 2.5F, true).run(); }

    public static void detonateLaser(ServerLevel level, Vec3 origin, Entity shot) {
        new ReactorExplosion(level, origin, 5, false, shot).run();
    }

    private void run() {
        if (EventHooks.onExplosionStart(level, event)) return;
        entities = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, bounds, LivingEntity::isAlive));
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, event, entities));
        int steps = (int)Math.ceil(Math.PI / Math.atan(1 / ReactorBlastMath.radius(power)));
        for (int i = 0; i < steps * 2; i++) for (int y = 0; y < steps; y++) ray(origin, Math.PI * 2 / steps * i, Math.PI / steps * y, power, i % 8 == 0 && y % 8 == 0);
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, event, new ArrayList<>()));
        for (var pos : new java.util.LinkedHashSet<>(event.getToBlow())) {
            var hit = blocks.get(pos);
            if (hit == null || !level.hasChunkAt(pos)) continue;
            if (hit.drops() && level.random.nextFloat() <= dropRate && hit.state().canDropFromExplosion(level, pos, event)) Block.dropResources(hit.state(), level, pos, level.getBlockEntity(pos));
            hit.state().onBlockExploded(level, pos, event);
        }
        for (var entry : motion.entrySet()) {
            var v = entry.getValue();
            entry.getKey().setDeltaMovement(Math.clamp(v.x, -500, 500), Math.clamp(v.y, -500, 500), Math.clamp(v.z, -500, 500));
            entry.getKey().hurtMarked = true;
        }
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4, (1 + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        if (!electrical && !laser) Radiation.exposeToFallout(level, origin);
    }

    private void ray(Vec3 start, double phi, double theta, double strength, boolean damageEntities) {
        var direction = new Vec3(Math.sin(theta) * Math.cos(phi), Math.cos(theta), Math.sin(theta) * Math.sin(phi));
        Vec3 point = start;
        int step = 0;
        while (point.y > level.getMinBuildHeight() && point.y < level.getMaxBuildHeight() && bounds.contains(point)) {
            var pos = BlockPos.containing(point);
            if (!level.hasChunkAt(pos)) return;
            var state = level.getBlockState(pos);
            double resistance = state.isAir() ? 0 : state.getExplosionResistance(level, pos, event);
            if (resistance < 0) return;
            double absorption = state.isAir() ? 0.5 : ReactorBlastMath.absorption(resistance);
            boolean bypass = absorption > 1000 && !state.is(Blocks.BEDROCK) && !state.is(PowerMachines.IRIDIUM_STONE.get());
            if (bypass) absorption = 0.5; else {
                if (absorption > strength) return;
                if (!state.isAir()) mark(pos, state, strength <= 8);
            }
            if (damageEntities && step % 8 == 0 && strength >= 0.25) hurt(point, strength);
            if (absorption > 10) for (int i = 0; i < 5; i++) ray(point, level.random.nextDouble() * Math.PI * 2, level.random.nextDouble() * Math.PI, strength - absorption, false);
            strength -= absorption;
            if (strength <= 0) return;
            point = point.add(direction);
            step++;
        }
    }

    private void mark(BlockPos pos, BlockState state, boolean drops) {
        var previous = blocks.get(pos);
        if (previous == null) event.getToBlow().add(pos);
        if (previous == null || !drops) blocks.put(pos, new Hit(state, drops));
    }

    private void hurt(Vec3 point, double strength) {
        var alive = new ArrayList<Entity>();
        for (var entity : entities) {
            if (!entity.ignoreExplosion(event) && entity.position().distanceTo(point) <= strength / 1.5) {
                double distance = entity.position().distanceTo(origin);
                entity.hurt(laser ? level.damageSources().generic() : electrical ? ElectricHazards.source(level) : Radiation.nuclearDamage(level), ReactorBlastMath.damage(strength, distance));
                if (!entity.isAlive()) {
                    motion.remove(entity);
                    continue;
                }
                var push = entity.position().subtract(origin).normalize().scale(ReactorBlastMath.force(strength, distance));
                motion.put(entity, motion.getOrDefault(entity, entity.getDeltaMovement()).add(push));
            }
            alive.add(entity);
        }
        entities = alive;
    }
}
