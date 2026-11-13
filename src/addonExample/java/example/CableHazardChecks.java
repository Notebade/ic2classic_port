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
    private static boolean sent;
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
        } finally { level.setBlockAndUpdate(cable, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(sink, Blocks.AIR.defaultBlockState()); }
        activeLevel = level; activeSource = source;
        var chunk = new net.minecraft.world.level.ChunkPos(source);
        alreadyForced = level.getForcedChunks().contains(chunk.toLong());
        level.setChunkForced(chunk.x, chunk.z, true);
        for (int x = 1; x <= 2; x++) level.setBlockAndUpdate(source.east(x), Ic2Api.block(ResourceLocation.parse("ic2:iron_cable")).orElseThrow().defaultBlockState());
        level.setBlockAndUpdate(source.east(3), Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow().defaultBlockState());
        target = new net.minecraft.world.entity.monster.Zombie(net.minecraft.world.entity.EntityType.ZOMBIE, level);
        target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.setPos(source.east().getCenter());
        level.addFreshEntity(target);
    }
    static boolean pending() { return target != null; }
    static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (target == null || event.getLevel() != activeLevel) return;
        if (!sent) {
            if (!activeLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(activeSource.east()).inflate(1)).contains(target)) {
                if (++waiting > 100) throw new AssertionError("shock fixture did not become visible in a forced chunk");
                return;
            }
            require(EuNetwork.send(activeLevel, activeSource, 64, java.util.Set.of(Direction.EAST), 0) == 64, "first shock packet transfers with loss");
            require(EuNetwork.send(activeLevel, activeSource, 64, java.util.Set.of(Direction.EAST), 0) == 64, "second shock packet transfers with loss");
            sent = true;
            return;
        }
        try {
            require(target.getHealth() == 18, "shock accumulates two packets, uses maximum exposure along each path, then ceil(EU/64); health=" + target.getHealth() + "; alive=" + target.isAlive() + "; removed=" + target.isRemoved());
            org.slf4j.LoggerFactory.getLogger(CableHazardChecks.class).info("IC2 cable hazards passed: overload, open circuit, path exposure and accumulated electric injury");
        } catch (Throwable error) {
            org.slf4j.LoggerFactory.getLogger(CableHazardChecks.class).error("IC2 API runtime smoke checks FAILED", error);
            throw error;
        } finally {
            target.discard(); target = null;
            for (int x = 1; x <= 3; x++) activeLevel.setBlockAndUpdate(activeSource.east(x), Blocks.AIR.defaultBlockState());
            if (!alreadyForced) {
                var chunk = new net.minecraft.world.level.ChunkPos(activeSource);
                activeLevel.setChunkForced(chunk.x, chunk.z, false);
            }
            activeLevel = null; activeSource = null;
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
