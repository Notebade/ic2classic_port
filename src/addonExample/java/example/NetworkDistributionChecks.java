package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

final class NetworkDistributionChecks {
    static void run(ServerLevel level) {
        var source = new BlockPos(568, 300, 568); level.getChunkAt(source); var placed = new java.util.HashSet<BlockPos>();
        try {
            for (var side : java.util.List.of(Direction.WEST, Direction.EAST)) {
                place(level, placed, source.relative(side), "iron_cable"); place(level, placed, source.relative(side, 2), "induction_furnace");
            }
            require(EuNetwork.send(level, source, 128, java.util.Set.of(Direction.WEST, Direction.EAST), 0) == 128, "equal-loss branches consume the one available packet");
            require(energy(level, source.west(2)) == 63 && energy(level, source.east(2)) == 63, "source inverse-loss allocation gives both demanding branches equal EU instead of starving the second");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            place(level, placed, source.west(), "iron_cable"); place(level, placed, source.west(2), "induction_furnace");
            for (int x = 1; x <= 3; x++) place(level, placed, source.east(x), "iron_cable");
            place(level, placed, source.east(4), "induction_furnace");
            require(EuNetwork.send(level, source, 128, java.util.Set.of(Direction.WEST, Direction.EAST), 0) == 128
                && energy(level, source.west(2)) == 95 && energy(level, source.east(4)) == 29, "source 1/loss weights split gross packet 96:32 then charge each path's integer loss");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            for (var side : Direction.Plane.HORIZONTAL) {
                place(level, placed, source.relative(side), "iron_cable"); place(level, placed, source.relative(side, 2), "induction_furnace");
            }
            require(EuNetwork.send(level, source, 512, java.util.Set.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST), 0) == 512, "high source packet is shared across four paths before voltage checks");
            for (var side : Direction.Plane.HORIZONTAL) require(energy(level, source.relative(side, 2)) == 127, "shared 512-EU source does not falsely overvolt a receiver of a 127-EU branch");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            for (var side : java.util.List.of(Direction.WEST, Direction.EAST)) {
                place(level, placed, source.relative(side), "iron_cable"); place(level, placed, source.relative(side, 2), "induction_furnace");
            }
            ((ic2.api.energy.EnergySink) level.getBlockEntity(source.west(2))).receiveEnergy(9999);
            require(EuNetwork.send(level, source, 128, java.util.Set.of(Direction.WEST, Direction.EAST), 0) == 128
                && energy(level, source.west(2)) == 10000 && energy(level, source.east(2)) == 124, "source redistribution fills a nearly full sink then reallocates the remainder with per-round path losses");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            var controller = source.west(2);
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) {
                var pos = controller.offset(-x, -y, z); place(level, placed, pos, pos.equals(controller) ? "pressure_alloy_furnace" : "pressure_alloy_furnace_structure");
            }
            ApiSmokeChecks.tick(level, controller, level.getBlockEntity(controller).getType());
            place(level, placed, source.west(), "iron_cable"); place(level, placed, source.south(), "iron_cable"); place(level, placed, source.south().west(), "iron_cable");
            place(level, placed, source.east(), "iron_cable"); place(level, placed, source.east(2), "induction_furnace");
            require(EuNetwork.send(level, source, 128, java.util.Set.of(Direction.WEST, Direction.SOUTH, Direction.EAST), 0) == 128
                && energy(level, controller) == 63 && energy(level, source.east(2)) == 63, "controller and linked part count as one demanding machine, retaining the shortest input path");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            place(level, placed, source, "batbox"); place(level, placed, source.north(), "iron_cable"); place(level, placed, source.north(2), "batbox");
            ((ic2.api.energy.EnergySink) level.getBlockEntity(source)).receiveEnergy(100);
            ApiSmokeChecks.tick(level, source, level.getBlockEntity(source).getType());
            require(energy(level, source) == 68 && energy(level, source.north(2)) == 31, "source permits equal-tier storage chaining through non-output faces without creating EU");
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); placed.clear();
            place(level, placed, source, "batbox");
            for (var pos : java.util.List.of(source.north(), source.north().east(), source.east())) place(level, placed, pos, "iron_cable");
            ((ic2.api.energy.EnergySink) level.getBlockEntity(source)).receiveEnergy(100);
            ApiSmokeChecks.tick(level, source, level.getBlockEntity(source).getType());
            require(energy(level, source) == 100, "a loop returning only to its own source consumes and creates no EU");
        } finally { for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); }
    }
    private static void place(ServerLevel level, java.util.Set<BlockPos> placed, BlockPos pos, String name) { level.getChunkAt(pos); placed.add(pos); level.setBlock(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState(), 2 | 16); }
    private static long energy(ServerLevel level, BlockPos pos) { return Ic2Api.machineInfo(level, pos).orElseThrow(() -> new AssertionError("receiver was incorrectly destroyed")).energyStored(); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
