package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class ChargingPadChecks {
    private ChargingPadChecks() {}

    static void run(ServerLevel level) {
        var pos = new BlockPos(112, 80, 112);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        try {
            level.setBlockAndUpdate(pos, Ic2Api.block(id("chargepad_hv")).orElseThrow().defaultBlockState());
            var entity = level.getBlockEntity(pos);
            var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            require(menu.slots.get(1).safeInsert(stack("field_expansion_pad_upgrade", 1)).isEmpty(), "projection slot accepts a field upgrade");
            require(menu.slots.get(2).safeInsert(stack("field_expansion_pad_upgrade", 1)).isEmpty(), "second field upgrade fits");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("range_milliblocks") == 2800, "duplicate field upgrades do not stack");
            require(!menu.slots.get(1).mayPlace(stack("upgrade_overclocker", 1)), "projection slot rejects machine upgrades");
            require(!menu.slots.get(3).mayPlace(stack("field_expansion_pad_upgrade", 1)), "machine slot rejects projection upgrades");
            menu.slots.get(3).safeInsert(stack("upgrade_energy_storage", 2));
            menu.slots.get(4).safeInsert(stack("upgrade_energy_storage_multiplier", 1));
            menu.slots.get(5).safeInsert(stack("upgrade_transformer", 2));
            var info = Ic2Api.machineInfo(level, pos).orElseThrow();
            require(info.energyCapacity() == 2040000 && info.values().get("packet_eu") == 8192, "stacked machine upgrades affect real storage and input tier");
            require(((EnergySink) entity).canReceiveEnergy(Direction.DOWN, 0, 8192), "upgraded tier accepts its packet below");
            require(!((EnergySink) entity).canReceiveEnergy(Direction.UP, 0, 8192), "upgrades retain bottom-only input");
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("chargepad_hv")).orElseThrow().defaultBlockState());
            level.getBlockEntity(pos).loadWithComponents(saved, level.registryAccess());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity() == 2040000, "upgrades survive reload");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(id("chargepad_fission")).orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos);
            menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            require(menu.slots.getFirst().safeInsert(stack("uranium_rod_single", 1)).isEmpty(), "fission pad accepts uranium fuel");
            for (int tick = 0; tick < 21; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 400, "one uranium cycle produces twenty source-rate pulses");
            require(menu.slots.getFirst().getItem().getDamageValue() == 1, "cycle consumes one rod durability");
        } finally {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ic2", path); }
    private static ItemStack stack(String path, int count) { return new ItemStack(Ic2Api.item(id(path)).orElseThrow(), count); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
