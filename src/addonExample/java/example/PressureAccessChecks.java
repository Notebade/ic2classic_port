package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;

final class PressureAccessChecks {
    static void run(ServerLevel level) {
        var origin = new BlockPos(440, 300, 440);
        level.getChunkAt(origin);
        var positions = new java.util.ArrayList<BlockPos>();
        try {
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) {
                var pos = origin.offset(-x, -y, z);
                positions.add(pos);
                String name = pos.equals(origin) ? "pressure_alloy_furnace" : "pressure_alloy_furnace_structure";
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState());
            }
            var part = origin.below();
            require(Ic2Api.machineInfo(level, part).isEmpty(), "reading a pressure part must not form the structure");
            require(!level.getBlockEntity(part).saveWithoutMetadata(level.registryAccess()).contains("controller"), "API reads do not claim pressure parts");
            var controller = level.getBlockEntity(origin);
            ApiSmokeChecks.tick(level, origin, controller.getType());
            require(Ic2Api.machineInfo(level, part).isPresent(), "the actual controller tick forms the pressure structure");
            var cached = level.getCapability(Capabilities.ItemHandler.BLOCK, origin, Direction.UP);
            require(cached != null && cached.getSlots() > 0, "formed pressure controller exposes inputs");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var menu = ((net.minecraft.world.MenuProvider) controller).createMenu(0, player.getInventory(), player);
            require(menu.slots.get(1).safeInsert(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 3)).isEmpty(), "pressure furnace accepts copper");
            require(menu.slots.get(2).safeInsert(new net.minecraft.world.item.ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow())).isEmpty(), "pressure furnace accepts tin");
            var hot = controller.saveWithoutMetadata(level.registryAccess());
            hot.putInt("heat", 10000); hot.putInt("energy", 10000);
            controller.loadWithComponents(hot, level.registryAccess());
            for (int tick = 0; tick < 12; tick++) ApiSmokeChecks.tick(level, origin, controller.getType());
            var output = menu.slots.get(3).getItem();
            require(output.is(Ic2Api.item(ResourceLocation.parse("ic2:ingot_bronze")).orElseThrow()) && output.getCount() == 4,
                "formed pressure furnace actually produces four bronze from three copper and one tin");
            require(menu.slots.get(1).getItem().isEmpty() && menu.slots.get(2).getItem().isEmpty(), "pressure alloying consumes both exact inputs");
            level.setBlockAndUpdate(origin.offset(-1, -1, 1), Blocks.AIR.defaultBlockState());
            require(Ic2Api.machineInfo(level, part).isEmpty(), "remote damage immediately revokes part telemetry");
            require(cached.getSlots() == 0, "remote damage immediately revokes cached controller automation");
        } finally {
            for (var pos : positions) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
