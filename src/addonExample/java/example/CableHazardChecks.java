package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

final class CableHazardChecks {
    private static net.minecraft.world.entity.monster.Zombie target;
    private static ServerLevel activeLevel;
    private static BlockPos activeSource;
    private static boolean alreadyForced;
    private enum Phase { BASE_SEND, BASE_VERIFY, WET_SEND, WET_VERIFY, DRY_SEND, DRY_VERIFY, LAVA_SEND, LAVA_VERIFY }
    private static Phase phase = Phase.BASE_SEND;
    private static int waiting;
    static void run(ServerLevel level) {
        var source = new BlockPos(452, 300, 452);
        level.getChunkAt(source);
        var cable = source.east(); var sink = source.east(2);
        try {
            level.setBlockAndUpdate(cable, Ic2Api.block(ResourceLocation.parse("ic2:tin_cable")).orElseThrow().defaultBlockState());
            level.setBlockAndUpdate(sink, Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow().defaultBlockState());
            require(EuNetwork.send(level, source, 6, java.util.Set.of(Direction.EAST), 0) == 6,
                "overload reaches the demanding sink before burning the conductor");
            require(level.getBlockState(cable).isAir(), "tin burns at the source threshold of six actually supplied EU");
            level.setBlockAndUpdate(cable, Ic2Api.block(ResourceLocation.parse("ic2:tin_cable")).orElseThrow().defaultBlockState());
            level.setBlockAndUpdate(sink, Blocks.AIR.defaultBlockState());
            require(EuNetwork.send(level, source, 128, java.util.Set.of(Direction.EAST), 0) == 0 && !level.getBlockState(cable).isAir(),
                "an open circuit consumes nothing and never burns merely from an offered packet");
            require(level.getBlockState(cable).getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer, "source cables support fluid logging for wet/lava shock behavior");
            var container = (net.minecraft.world.level.block.LiquidBlockContainer) level.getBlockState(cable).getBlock();
            require(container.placeLiquid(level, cable, level.getBlockState(cable), net.minecraft.world.level.material.Fluids.WATER.getSource(false)), "water bucket logs a real cable");
            require(!container.placeLiquid(level, cable, level.getBlockState(cable), net.minecraft.world.level.material.Fluids.LAVA.getSource(false)), "logging cannot replace an already stored fluid");
            var pickup = (net.minecraft.world.level.block.BucketPickup) level.getBlockState(cable).getBlock();
            require(pickup.pickupBlock(null, level, cable, level.getBlockState(cable)).is(net.minecraft.world.item.Items.WATER_BUCKET), "bucket pickup returns logged water and preserves the conductor");
            require(container.placeLiquid(level, cable, level.getBlockState(cable), net.minecraft.world.level.material.Fluids.LAVA.getSource(false))
                && pickup.pickupBlock(null, level, cable, level.getBlockState(cable)).is(net.minecraft.world.item.Items.LAVA_BUCKET), "lava logging and pickup also preserve their fluid type");
            container.placeLiquid(level, cable, level.getBlockState(cable), net.minecraft.world.level.material.Fluids.WATER.getSource(false));
            level.setBlockAndUpdate(sink, Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow().defaultBlockState());
            require(EuNetwork.send(level, source, 6, java.util.Set.of(Direction.EAST), 0) == 6 && level.getBlockState(cable).is(net.minecraft.world.level.block.Blocks.WATER), "burning a fluid-logged conductor leaves its fluid in the world");
        } finally { level.setBlockAndUpdate(cable, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(sink, Blocks.AIR.defaultBlockState()); }
        activeLevel = level; activeSource = source;
        var chunk = new net.minecraft.world.level.ChunkPos(source);
        alreadyForced = level.getForcedChunks().contains(chunk.toLong());
        level.setChunkForced(chunk.x, chunk.z, true);
        for (int x = 1; x <= 2; x++) level.setBlockAndUpdate(source.east(x), Ic2Api.block(ResourceLocation.parse("ic2:iron_cable")).orElseThrow().defaultBlockState());
        level.setBlockAndUpdate(source.east(3), Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow().defaultBlockState());
        target = new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
        target.setNoAi(true); target.setNoGravity(true);
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARVED_PUMPKIN));
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.setPos(source.east().getCenter());
        level.addFreshEntity(target);
    }
    static boolean pending() { return target != null; }
    static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (target == null || event.getLevel() != activeLevel) return;
        boolean done = false;
        try { done = advance(); }
        catch (Throwable error) { done = true; org.slf4j.LoggerFactory.getLogger(CableHazardChecks.class).error("IC2 API runtime smoke checks FAILED", error); throw error; }
        finally { if (done) cleanup(); }
    }
    private static boolean advance() {
        if (phase == Phase.BASE_SEND) {
            if (!activeLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(activeSource.east()).inflate(1)).contains(target)) {
                if (++waiting > 100) throw new AssertionError("shock fixture did not become visible in a forced chunk");
                return false;
            }
            send(); send(); phase = Phase.BASE_VERIFY; return false;
        }
        if (phase == Phase.BASE_VERIFY) {
            require(target.getHealth() == 18, "shock accumulates two packets, uses maximum exposure along each path, then ceil(EU/64); health=" + target.getHealth() + "; alive=" + target.isAlive() + "; removed=" + target.isRemoved());
            activeLevel.setWeatherParameters(6000, 0, false, false);
            var cable = activeSource.east(); var state = activeLevel.getBlockState(cable);
            ((net.minecraft.world.level.block.LiquidBlockContainer) state.getBlock()).placeLiquid(activeLevel, cable, state, net.minecraft.world.level.material.Fluids.WATER.getSource(false));
            activeLevel.setBlock(activeSource.south(3), Blocks.WATER.defaultBlockState(), 2 | 16);
            moveVictim(activeSource.south(3)); waiting = 2; phase = Phase.WET_SEND; return false;
        }
        if (phase == Phase.WET_SEND || phase == Phase.DRY_SEND || phase == Phase.LAVA_SEND) {
            if (waiting-- > 0) return false;
            if (phase == Phase.WET_SEND) { require(target.isInWaterOrRain(), "fixture wet target is immersed by actual world ticks"); send(); send(); phase = Phase.WET_VERIFY; }
            else if (phase == Phase.DRY_SEND) { require(!target.isInWaterOrRain(), "fixture dry target has left actual water"); send(); phase = Phase.DRY_VERIFY; }
            else { send(); phase = Phase.LAVA_VERIFY; }
            return false;
        }
        if (phase == Phase.WET_VERIFY) {
            require(target.getHealth() == 18, "waterlogged cable shocks a wet target beyond ordinary one-block contact range");
            activeLevel.setBlock(activeSource.south(3), Blocks.AIR.defaultBlockState(), 2 | 16);
            moveVictim(activeSource.south(3)); waiting = 2; phase = Phase.DRY_SEND; return false;
        }
        if (phase == Phase.DRY_VERIFY) {
            require(target.getHealth() == 20, "waterlogged cable does not extend injury to a dry distant target");
            var cable = activeSource.east(); var state = activeLevel.getBlockState(cable);
            ((net.minecraft.world.level.block.BucketPickup) state.getBlock()).pickupBlock(null, activeLevel, cable, state);
            state = activeLevel.getBlockState(cable);
            ((net.minecraft.world.level.block.LiquidBlockContainer) state.getBlock()).placeLiquid(activeLevel, cable, state, net.minecraft.world.level.material.Fluids.LAVA.getSource(false));
            activeLevel.setBlock(activeSource.east(2), Ic2Api.block(ResourceLocation.parse("ic2:iron_triple_insulated_cable")).orElseThrow().defaultBlockState(), 2 | 16);
            moveVictim(activeSource.east().south()); waiting = 2; phase = Phase.LAVA_SEND; return false;
        }
        require(target.getHealth() == 20, "lava-logged conductor uses its own block volume instead of the normal inflated contact box");
        org.slf4j.LoggerFactory.getLogger(CableHazardChecks.class).info("IC2 cable hazards passed: overload, packet exposure, logged fluids, wet extension and lava contact limits");
        return true;
    }
    private static void send() { require(EuNetwork.send(activeLevel, activeSource, 64, java.util.Set.of(Direction.EAST), 0) == 64, "shock packet transfers with exact loss"); }
    private static void moveVictim(BlockPos pos) { target.setHealth(20); target.invulnerableTime = 0; target.setRemainingFireTicks(0); target.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); }
    private static void cleanup() {
            target.discard(); target = null;
            for (int x = 1; x <= 3; x++) activeLevel.setBlockAndUpdate(activeSource.east(x), Blocks.AIR.defaultBlockState());
            for (var pos : BlockPos.betweenClosed(activeSource.offset(-2, -2, -2), activeSource.offset(5, 3, 5)))
                if (activeLevel.hasChunkAt(pos) && activeLevel.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) activeLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            if (!alreadyForced) {
                var chunk = new net.minecraft.world.level.ChunkPos(activeSource);
                activeLevel.setChunkForced(chunk.x, chunk.z, false);
            }
            activeLevel = null; activeSource = null;
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
