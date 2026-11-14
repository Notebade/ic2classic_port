package ic2.core.equipment;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LaserShot extends Entity {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "ic2");
    static final Supplier<EntityType<LaserShot>> TYPE = TYPES.register("mining_laser", () ->
        EntityType.Builder.<LaserShot>of(LaserShot::new, MobCategory.MISC).sized(.2f, .2f)
            .clientTrackingRange(8).updateInterval(1).build("ic2:mining_laser"));
    private static final TagKey<net.minecraft.world.level.block.Block> UNBREAKABLE =
        TagKey.create(Registries.BLOCK, ResourceLocation.parse("ic2:mining_laser_unbreakable"));

    private UUID owner;
    private ServerPlayer shooter;
    private UUID target;
    private float range;
    private float power;
    private int blocks;
    private int age;
    private boolean explosive;
    private boolean smelt;

    private LaserShot(EntityType<?> type, Level level) {
        super(type, level);
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    static void spawn(ServerPlayer player, Vec3 origin, Vec3 direction, String mode, Entity target) {
        var shot = new LaserShot(TYPE.get(), player.level());
        shot.owner = player.getUUID();
        shot.shooter = player;
        shot.target = target == null ? null : target.getUUID();
        shot.range = switch (mode) { case "low_focus" -> 1.1f; case "horizontal" -> 1.5f; case "tracking" -> 4; default -> 2; };
        shot.power = switch (mode) {
            case "long_range" -> 32;
            case "super_heat" -> 8;
            case "scatter", "explosive" -> 12;
            case "tracking" -> 64;
            default -> 5;
        };
        shot.blocks = switch (mode) { case "tracking" -> 5; case "low_focus" -> 1; default -> Integer.MAX_VALUE; };
        shot.explosive = mode.equals("explosive");
        shot.smelt = mode.equals("super_heat");
        shot.setPos(origin);
        shot.aim(direction.normalize());
        player.serverLevel().addFreshEntity(shot);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (shooter == null && owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer player) shooter = player;
        if (shooter == null || shooter.isRemoved() || shooter.level() != level) {
            discard();
            return;
        }
        if (range < 1 || power <= 0 || blocks <= 0) {
            if (explosive) explode(level, position());
            discard();
            return;
        }
        if (target != null) {
            Entity tracked = level.getEntity(target);
            if (tracked == null || !tracked.isAlive()) {
                discard();
                return;
            }
            aim(tracked.getBoundingBox().getCenter().subtract(position()).normalize());
        }
        advance(level, shooter);
    }

    private void aim(Vec3 motion) {
        setDeltaMovement(motion);
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float) Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
    }

    private void advance(ServerLevel level, ServerPlayer player) {
        age++;
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) {
            discard();
            return;
        }
        var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 edge = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        var entity = ProjectileUtil.getEntityHitResult(level, this, start, edge,
            getBoundingBox().expandTowards(motion).inflate(.3), e -> e.isPickable() && (e != player || age >= 5));
        Vec3 impact = entity == null ? edge : entity.getLocation();
        if (entity != null) {
            var hit = entity.getEntity();
            if (!(hit instanceof Player other) || player.canHarmPlayer(other)) {
                if (explosive) explode(level, impact);
                else hurt(level, player, hit);
            }
            discard();
            return;
        }
        if (block.getType() != HitResult.Type.MISS) {
            if (explosive) {
                explode(level, impact);
                discard();
                return;
            }
            var pos = block.getBlockPos();
            var state = level.getBlockState(pos);
            if (!state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS)
                    && !state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)) {
                power -= .15f * state.getBlock().getExplosionResistance();
                if (power >= 0 && (state.is(UNBREAKABLE) || !MiningLaser.breakBlock(player, pos, smelt, () -> power = 0))) {
                    discard();
                    return;
                }
                blocks--;
            }
        } else {
            power -= .5f;
        }
        setPos(end);
        range -= .02f;
        if (level.getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER)) discard();
    }

    private void hurt(ServerLevel level, ServerPlayer player, Entity hit) {
        int damage = (int) power;
        if (damage <= 0) return;
        hit.setRemainingFireTicks(Math.max(hit.getRemainingFireTicks(), damage * (smelt ? 40 : 20)));
        hit.hurt(level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.ARROW,
            this, hit instanceof EnderMan ? this : player), damage);
    }

    private void explode(ServerLevel level, Vec3 pos) {
        ic2.core.machine.ReactorExplosion.detonateLaser(level, pos, this);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        target = tag.hasUUID("target") ? tag.getUUID("target") : null;
        range = tag.getFloat("range");
        power = tag.getFloat("power");
        blocks = tag.getInt("blocks");
        age = tag.getInt("age");
        explosive = tag.getBoolean("explosive");
        smelt = tag.getBoolean("smelt");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner);
        if (target != null) tag.putUUID("target", target);
        tag.putFloat("range", range);
        tag.putFloat("power", power);
        tag.putInt("blocks", blocks);
        tag.putInt("age", age);
        tag.putBoolean("explosive", explosive);
        tag.putBoolean("smelt", smelt);
    }
}
