package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Exercise actual registered block tickers and their player-facing inventories. */
final class AdvancedProductionChecks {
    static void run(ServerLevel level) {
        check(level, "induction_furnace", "minecraft:raw_iron", 1, "minecraft:iron_ingot", 1, 12, 3);
        check(level, "induction_blast_furnace", "minecraft:raw_iron", 1, "minecraft:iron_ingot", 1, 6, 3);
        check(level, "induction_smoker", "minecraft:beef", 1, "minecraft:cooked_beef", 1, 6, 3);
        check(level, "rotary_macerator", "minecraft:iron_ingot", 1, "ic2:dust_iron", 1, 12, 2);
        check(level, "centrifugal_extractor", "ic2:sticky_resin", 1, "ic2:rubber", 3, 12, 2);
        check(level, "singularity_compressor", "ic2:ingot_advanced_alloy", 1, "ic2:plate_advanced_alloy", 1, 12, 2);
        check(level, "compacting_recycler", "ic2:scrap", 9, "ic2:scrapbox", 1, 72, 2);
        check(level, "slow_grinder", "minecraft:cobblestone", 1, "ic2:scrap", 1, 100, 10);
        check(level, "centrifugal_rare_earth_extractor", "minecraft:clay_ball", 2, "ic2:dust_aluminium", 2, 6, 2);
    }
    private static void check(ServerLevel level, String name, String ingredient, int count, String result, int amount, int ticks, int output) {
        var pos = new BlockPos(444, 300, 444);
        level.getChunkAt(pos);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous = player.position();
        try {
            player.setPos(pos.getCenter());
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState());
            var entity = level.getBlockEntity(pos);
            var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            require(menu.slots.get(1).safeInsert(new ItemStack(Ic2Api.item(ResourceLocation.parse(ingredient)).orElseThrow(), count)).isEmpty(), name + " accepts recipe input");
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            saved.putInt("heat", 10000);
            saved.putInt("energy", Math.toIntExact(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity()));
            entity.loadWithComponents(saved, level.registryAccess());
            for (int tick = 0; tick < ticks / 2; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            saved = entity.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos);
            entity.loadWithComponents(saved, level.registryAccess());
            menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            for (int tick = ticks / 2; tick < ticks; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            var actual = menu.slots.get(output).getItem();
            require(actual.is(Ic2Api.item(ResourceLocation.parse(result)).orElseThrow()) && actual.getCount() == amount,
                name + " completes the exact recipe after mid-cycle reload; actual=" + actual);
            require(menu.slots.get(1).getItem().isEmpty(), name + " consumes exactly the input");
        } finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); player.setPos(previous); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
