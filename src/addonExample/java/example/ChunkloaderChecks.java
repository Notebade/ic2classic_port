package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;

final class ChunkloaderChecks {
    private ChunkloaderChecks() {}

    static void run(ServerLevel level) {
        var first = new BlockPos(224, 300, 224);
        var second = first.east(2);
        level.getChunkAt(first);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        int baseline = tickets(level);
        try {
            player.setPos(first.getX(), first.getY(), first.getZ());
            for (var pos : java.util.List.of(first, second)) {
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:chunkloader")).orElseThrow().defaultBlockState());
                var entity = level.getBlockEntity(pos);
                var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
                require(menu.clickMenuButton(player, 16), "owner configures radius zero through the server menu");
                for (int tick = 0; tick < 4; tick++) ((EnergySink) entity).receiveEnergy(128);
                ApiSmokeChecks.tick(level, pos, entity.getType());
                var snapshot = Ic2Api.machineInfo(level, pos).orElseThrow();
                require(snapshot.active() && snapshot.energyStored() == 507 && snapshot.values().get("forced_chunks") == 1, "startup uses real EU and one ticket at radius zero");
            }
            require(tickets(level) == baseline + 2, "two loaders own separate tickets even in the same chunk");
            level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
            require(tickets(level) == baseline + 1, "breaking one loader preserves the other's ticket");
            var entity = level.getBlockEntity(second);
            for (int tick = 0; tick < 103; tick++) ApiSmokeChecks.tick(level, second, entity.getType());
            require(!Ic2Api.machineInfo(level, second).orElseThrow().active() && tickets(level) == baseline, "power exhaustion releases exactly the loader's tickets");
        } finally {
            level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
            player.setPos(previous);
        }
    }

    private static int tickets(ServerLevel level) {
        var data = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks");
        return data.getBlockForcedChunks().getChunks().values().stream().mapToInt(java.util.Set::size).sum()
            + data.getBlockForcedChunks().getTickingChunks().values().stream().mapToInt(java.util.Set::size).sum();
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
