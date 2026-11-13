package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Three actual server processes share one isolated world to test persisted tickets. */
final class ChunkloaderRestartChecks {
    private static final BlockPos OWNER = new BlockPos(384, 300, 384), INVALID = OWNER.east(64);
    private static final TicketController TICKETS = new TicketController(ResourceLocation.parse("ic2:chunkloader"));
    private ChunkloaderRestartChecks() {}
    static void run(ServerStartedEvent event) {
        var server = event.getServer();
        String phase = System.getProperty("ic2.chunkloader.phase");
        try {
            var level = server.overworld();
            if (phase.equals("save")) save(level);
            else if (phase.equals("restore")) restore(level);
            else if (phase.equals("ticking")) ticking(level);
            else throw new AssertionError("Unknown restart phase " + phase);
            com.mojang.logging.LogUtils.getLogger().info("IC2 chunkloader restart checks passed: {}", phase);
        } catch (Throwable error) { com.mojang.logging.LogUtils.getLogger().error("IC2 chunkloader restart checks FAILED: " + phase, error); }
        finally { server.halt(false); }
    }
    private static void save(ServerLevel level) {
        level.getChunkAt(OWNER); level.getChunkAt(INVALID);
        level.setBlockAndUpdate(OWNER, Blocks.AIR.defaultBlockState());
        TICKETS.forceChunk(level, INVALID, INVALID.getX() >> 4, INVALID.getZ() >> 4, false, false);
        level.setBlockAndUpdate(OWNER, Ic2Api.block(ResourceLocation.parse("ic2:chunkloader")).orElseThrow().defaultBlockState());
        var machine = level.getBlockEntity(OWNER);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(OWNER.getCenter());
        var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
        menu.clickMenuButton(player, 16);
        for (int packet = 0; packet < 500; packet++) ((EnergySink) machine).receiveEnergy(128);
        ApiSmokeChecks.tick(level, OWNER, machine.getType());
        require(Ic2Api.machineInfo(level, OWNER).orElseThrow().active(), "radius-zero loader activates before shutdown");
        TICKETS.forceChunk(level, OWNER, (OWNER.getX() >> 4) + 5, OWNER.getZ() >> 4, true, false);
        TICKETS.forceChunk(level, OWNER, OWNER.getX() >> 4, OWNER.getZ() >> 4, true, true);
        TICKETS.forceChunk(level, INVALID, INVALID.getX() >> 4, INVALID.getZ() >> 4, true, false);
        require(nonTicking(level) == 3 && tickingCount(level) == 1, "persist a valid ticket plus wrong radius, wrong mode and missing-controller tickets");
    }
    private static void restore(ServerLevel level) {
        require(level.hasChunkAt(OWNER), "valid persistent ticket loads its owner on a fresh server");
        require(nonTicking(level) == 1 && tickingCount(level) == 0, "startup prunes missing owners and wrong radius/ticking modes");
        var machine = level.getBlockEntity(OWNER);
        require(machine != null && Ic2Api.machineInfo(level, OWNER).orElseThrow().active(), "loader state survives shutdown");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setPos(OWNER.getCenter());
        var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
        menu.clickMenuButton(player, 20);
        ApiSmokeChecks.tick(level, OWNER, machine.getType());
        require(nonTicking(level) == 0 && tickingCount(level) == 1, "mode change replaces the old ticket with a ticking ticket");
    }
    private static void ticking(ServerLevel level) {
        require(level.hasChunkAt(OWNER) && nonTicking(level) == 0 && tickingCount(level) == 1, "ticking mode and ticket survive another fresh server process");
        require(Ic2Api.machineInfo(level, OWNER).orElseThrow().values().get("chunk_processing") == 1, "restored GUI/API setting matches the actual ticket mode");
        level.setBlockAndUpdate(OWNER, Blocks.AIR.defaultBlockState());
        require(nonTicking(level) == 0 && tickingCount(level) == 0, "breaking the restored loader leaves no tickets");
    }
    private static ForcedChunksSavedData data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks"); }
    private static int nonTicking(ServerLevel level) { return data(level).getBlockForcedChunks().getChunks().values().stream().mapToInt(java.util.Set::size).sum(); }
    private static int tickingCount(ServerLevel level) { return data(level).getBlockForcedChunks().getTickingChunks().values().stream().mapToInt(java.util.Set::size).sum(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
