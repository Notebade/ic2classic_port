package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class ChargedElectrolyzerChecks {
    static void run(ServerLevel level) {
        var pos = new BlockPos(460, 300, 460);
        level.getChunkAt(pos);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        try {
            place(level, pos, "charged_electrolyzer");
            place(level, pos.west(), "mfe"); place(level, pos.east(), "mfsu");
            setEnergy(level, pos.west(), 600000); setEnergy(level, pos.east(), 10000000);
            var machine = level.getBlockEntity(pos);
            var menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            require(menu.slots.get(0).safeInsert(stack("cell_water")).isEmpty(), "charged electrolyzer accepts a water cell");
            for (int tick = 0; tick < 125; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(energy(level, pos) == 5000, "charged electrolyzer simultaneously draws 8 and 32 EU from two storage tiers");
            var saved = machine.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); place(level, pos, "charged_electrolyzer");
            machine = level.getBlockEntity(pos); machine.loadWithComponents(saved, level.registryAccess());
            menu = ((MenuProvider) machine).createMenu(0, player.getInventory(), player);
            for (int tick = 125; tick < 500; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(menu.slots.get(0).getItem().isEmpty() && menu.slots.get(1).getItem().is(stack("cell_electrolyzed_water").getItem()), "reload preserves charging progress and completes one cell");
            require(energy(level, pos.west()) + energy(level, pos.east()) == 10600000 - 20000, "charging conserves the full 20000 EU across both storages");
            setEnergy(level, pos.west(), 0); setEnergy(level, pos.east(), 0);
            for (int tick = 0; tick < 500; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(energy(level, pos.west()) + energy(level, pos.east()) == 16000 && energy(level, pos) == 0, "discharge returns exactly the source tier-two 16000 EU without duplicating the buffer");
            require(menu.slots.get(0).getItem().is(stack("cell_water").getItem()) && menu.slots.get(1).getItem().isEmpty(), "discharge restores the original water cell");
        } finally { for (var at : java.util.List.of(pos, pos.west(), pos.east())) level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState()); }
    }
    private static void place(ServerLevel level, BlockPos pos, String name) { level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState()); }
    private static ItemStack stack(String name) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).orElseThrow()); }
    private static long energy(ServerLevel level, BlockPos pos) { return Ic2Api.machineInfo(level, pos).orElseThrow().energyStored(); }
    private static void setEnergy(ServerLevel level, BlockPos pos, int amount) {
        var entity = level.getBlockEntity(pos); var tag = entity.saveWithoutMetadata(level.registryAccess());
        tag.putInt("energy", amount); entity.loadWithComponents(tag, level.registryAccess());
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
