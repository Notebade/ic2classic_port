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
import net.neoforged.neoforge.capabilities.Capabilities;

final class RangedPumpChecks {
    static void run(ServerLevel level) {
        var pos = new BlockPos(500, 300, 500); level.getChunkAt(pos);
        var block = Ic2Api.block(ResourceLocation.parse("ic2:ranged_pump"));
        require(block.isPresent(), "working ranged pump completes the original fisher crafting dependency");
        craft(level, "overclocked_pump", "heat_storage_single", "upgrade_overclocker", "heat_storage_single", "upgrade_overclocker", "pump", "upgrade_overclocker", "heat_storage_single", "advanced_machine_block", "heat_storage_single");
        craft(level, "miner", "circuit", "machine_block", "circuit", "", "miningpipe_slim", "", "", "miningpipe_slim", "");
        craft(level, "ranged_pump", "plate_advanced_alloy", "overclocked_pump", "plate_advanced_alloy", "advanced_circuit", "miner", "advanced_circuit", "plate_advanced_alloy", "magnet", "plate_advanced_alloy");
        craft(level, "electric_fisher", "complex_circuit", "minecraft:hopper", "complex_circuit", "minecraft:fishing_rod", "plasma_core", "minecraft:fishing_rod", "stabilized_machine_block", "ranged_pump", "stabilized_machine_block");
        try {
            level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState()); level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState());
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var entity = level.getBlockEntity(pos); var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            var scanner = stack("fluid_scanner"); ((ic2.core.item.RechargeableItem) scanner.getItem()).setCharge(scanner, 10000);
            menu.slots.get(2).safeInsert(scanner); menu.slots.get(1).safeInsert(stack("miningpipe_slim").copyWithCount(2));
            ((EnergySink) entity).receiveEnergy(5000);
            for (int tick = 0; tick < 4; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            var saved = entity.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            entity = level.getBlockEntity(pos); entity.loadWithComponents(saved, level.registryAccess()); menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            for (int tick = 0; tick < 3; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
            require(tank != null && tank.getFluidInTank(0).getAmount() == 1000 && level.getBlockState(pos.below()).getBlock() instanceof ic2.core.machine.MiningPipeBlock,
                "ranged pump collects one source and installs its pipe across a mid-operation reload");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 4940 && menu.slots.get(1).getItem().getCount() == 1, "source operation costs exactly 60 machine EU and one vertical pipe");
            level.setBlock(pos.below(2), Blocks.BEDROCK.defaultBlockState(), 2 | 16);
            level.setBlock(pos.east(2).below(), Blocks.WATER.defaultBlockState(), 2 | 16);
            level.setBlock(pos.east(3).below(), Blocks.WATER.defaultBlockState(), 2 | 16);
            var distant = pos.offset(20, 0, 20); boolean distantLoaded = level.hasChunkAt(distant);
            ApiSmokeChecks.tick(level, pos, entity.getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("searching") == 1, "source search is budgeted over actual controller ticks");
            for (int tick = 0; tick < 300 && tank.getFluidInTank(0).getAmount() < 3000; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(tank.getFluidInTank(0).getAmount() == 3000 && level.getBlockState(pos.east(2).below()).getFluidState().isEmpty()
                && level.getBlockState(pos.east(3).below()).getFluidState().isEmpty(), "ranged scan collects two remote water sources without extra vertical pipes");
            require(menu.slots.get(1).getItem().getCount() == 1 && (distantLoaded || !level.hasChunkAt(distant)), "remote fluid collection conserves pipes and never loads distant search chunks");
            long beforeRetraction = Ic2Api.machineInfo(level, pos).orElseThrow().energyStored();
            menu.slots.get(2).setByPlayer(ItemStack.EMPTY);
            for (int tick = 0; tick < 5; tick++) ApiSmokeChecks.tick(level, pos, entity.getType());
            require(level.getBlockState(pos.below()).isAir() && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == beforeRetraction - 10, "removing scanner retracts a pipe in five ticks at two EU per tick");
        } finally { for (var at : java.util.List.of(pos, pos.below(), pos.below(2), pos.east(2).below(), pos.east(3).below())) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2 | 16); }
    }
    private static ItemStack stack(String name) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + name)).orElseThrow()); }
    private static void craft(ServerLevel level, String result, String... names) {
        var inputs = java.util.Arrays.stream(names).map(name -> name.isEmpty() ? ItemStack.EMPTY : new ItemStack(Ic2Api.item(ResourceLocation.parse(name.contains(":") ? name : "ic2:" + name)).orElseThrow())).toList();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
        var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow(() -> new AssertionError("original acquisition pattern missing: " + result));
        require(recipe.value().assemble(input, level.registryAccess()).is(stack(result).getItem()), "original acquisition pattern produces " + result);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
