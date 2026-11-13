package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import ic2.api.energy.EuNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

final class OvervoltageChecks {
    static void run(ServerLevel level) {
        var pos = new BlockPos(511, 300, 511); level.getChunkAt(pos);
        var outside = pos.east(); boolean loaded = level.hasChunkAt(outside);
        var block = Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow();
        try {
            level.setBlock(pos, block.defaultBlockState(), 2 | 16);
            require(EuNetwork.send(level, pos.west(), 512, java.util.Set.of(Direction.EAST), 0) == 0, "source overvoltage destruction does not claim an accepted energy packet");
            require(level.getBlockState(pos).isAir(), "a demanding MV induction furnace explodes from an HV packet");
            require(loaded || !level.hasChunkAt(outside), "electrical explosion never loads an adjacent missing chunk");
            level.setBlock(pos, block.defaultBlockState(), 2 | 16);
            ((EnergySink) level.getBlockEntity(pos)).receiveEnergy(9999);
            require(EuNetwork.send(level, pos.west(), 129, java.util.Set.of(Direction.EAST), 0) == 0 && level.getBlockState(pos).isAir(), "overvoltage is based on offered voltage even when only one EU would fit");
            level.setBlock(pos, block.defaultBlockState(), 2 | 16);
            ((EnergySink) level.getBlockEntity(pos)).receiveEnergy(10000);
            require(EuNetwork.send(level, pos.west(), 512, java.util.Set.of(Direction.EAST), 0) == 0 && level.getBlockState(pos).is(block), "a full sink has no demand and does not explode from an idle offer");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); level.setBlock(pos, block.defaultBlockState(), 2 | 16);
            level.setBlock(pos.west(), Ic2Api.block(ResourceLocation.parse("ic2:iron_cable")).orElseThrow().defaultBlockState(), 2 | 16);
            require(EuNetwork.send(level, pos.west(2), 129, java.util.Set.of(Direction.EAST), 0) == 129
                && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 128, "cable loss reduces actual incoming voltage before the native sink limit check");
            level.setBlock(pos, AddonEnergyFixtures.LEGACY.get().defaultBlockState(), 2 | 16);
            require(EuNetwork.send(level, pos.west(2), 129, java.util.Set.of(Direction.EAST), 0) == 0 && level.getBlockState(pos).is(AddonEnergyFixtures.LEGACY.get()), "legacy addon sinks keep rejection semantics and are never automatically destroyed");
            level.setBlock(pos, AddonEnergyFixtures.FUSE.get().defaultBlockState(), 2 | 16);
            require(EuNetwork.send(level, pos.west(2), 129, java.util.Set.of(Direction.EAST), 0) == 0
                && ((AddonEnergyFixtures.FusedSink) level.getBlockEntity(pos)).trippedPacket == 128, "real addon overvoltage callback receives actual post-loss voltage and handles its own fuse");
            require(EuNetwork.send(level, pos.west(2), 16, java.util.Set.of(Direction.EAST), 0) == 0, "tripped addon fuse rejects further energy without IC2 destroying its block");
        } finally { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), 2 | 16); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
