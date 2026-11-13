package example;

import ic2.api.Ic2Api;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;

/**
 * Real chunk unloading runs between server ticks, outside the synchronous smoke fixtures.
 */
final class StructureChunkChecks {
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET = net.minecraft.server.level.TicketType.create("ic2_verification", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
    private static StructureChunkChecks running;
    private final MinecraftServer server;
    private final ServerLevel level;
    private final Set<BlockPos> placed = new HashSet<>();
    private final BlockPos controller = new BlockPos(16015, 300, 16015);
    private final BlockPos part = controller.south();
    private IFluidHandler cached;
    private int age;
    private boolean unloaded;

    private StructureChunkChecks(MinecraftServer server) {
        this.server = server;
        level = server.overworld();
    }

    static void start(MinecraftServer server) {
        running = new StructureChunkChecks(server);
        running.prepare();
    }

    private void prepare() {
        var chunk = new net.minecraft.world.level.ChunkPos(controller);
        level.getChunkSource().addRegionTicket(TICKET, chunk, 0, chunk);
        for (int i = 0; i < 8; i++) {
            BlockPos pos = controller.offset(-(i % 2), -(i / 4), i / 2 % 2);
            level.getChunkAt(pos);
            place(pos, i == 0 ? "fuel_boiler" : "fuel_boiler_structure");
        }
        ApiSmokeChecks.tick(level, controller, level.getBlockEntity(controller).getType());
        require(formed(), "boiler forms before a real chunk unload");
        cached = level.getCapability(Capabilities.FluidHandler.BLOCK, part, Direction.UP);
        require(cached != null && cached.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE) == 2000, "fixture receives water before unloading");
    }

    static void tick(ServerTickEvent.Post event) {
        if (running == null) return;
        var check = running;
        try {
            check.advance();
        } catch (Throwable error) {
            com.mojang.logging.LogUtils.getLogger().error("IC2 API runtime smoke checks FAILED", error);
            check.finish();
        }
    }

    private void advance() {
        age++;
        if (!unloaded) {
            if (!level.hasChunkAt(part)) {
                unloaded = true;
                ApiSmokeChecks.tick(level, controller, level.getBlockEntity(controller).getType());
                require(!formed(), "controller stops after an actual structure chunk unload");
                require(cached.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "cached capability rejects an actually unloaded structure");
                require(!level.hasChunkAt(part), "capability and telemetry do not reload the missing chunk");
                level.getChunkAt(part);
            } else if (age > 1000) {
                throw new AssertionError("structure chunk did not unload within 1000 server ticks");
            }
            return;
        }
        ApiSmokeChecks.tick(level, controller, level.getBlockEntity(controller).getType());
        require(formed(), "controller reforms after a real chunk reload");
        var restored = level.getCapability(Capabilities.FluidHandler.BLOCK, part, Direction.UP);
        require(restored != null && restored.getFluidInTank(0).getAmount() == 2000, "fluid survives actual chunk unload and reload");
        com.mojang.logging.LogUtils.getLogger().info("IC2 API runtime smoke checks passed (including actual chunk unload/reload)");
        finish();
    }

    private boolean formed() {
        return Ic2Api.machineInfo(level, controller).orElseThrow().values().get("formed") == 1;
    }

    private void place(BlockPos pos, String id) {
        placed.add(pos);
        BlockState state = Ic2Api.block(ResourceLocation.parse("ic2:" + id)).orElseThrow().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, state);
    }

    private void finish() {
        running = null;
        for (var pos : placed) {
            level.getChunkAt(pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        var chunk = new net.minecraft.world.level.ChunkPos(controller);
        level.getChunkSource().removeRegionTicket(TICKET, chunk, 0, chunk);
        server.halt(false);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
