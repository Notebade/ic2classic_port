package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EuNetwork;
import ic2.api.machine.MachineSnapshot;
import ic2.api.recipe.Ic2Recipes;
import ic2.api.recipe.ProcessingRecipe.Process;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in runtime checks for an isolated dev-server world, never included in IC2 itself. */
public final class ApiSmokeChecks {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private ApiSmokeChecks() {}
    public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            RecipeAvailabilityChecks.run(server.overworld());
            ChargingPadChecks.run(server.overworld());
            UtilityMachineChecks.run(server.overworld());
            ChunkloaderChecks.run(server.overworld());
            ColossalChecks.run(server.overworld());
            EnchanterChecks.run(server.overworld());
            FisherChecks.run(server.overworld());
            AdvancedUpgradeChecks.run(server.overworld());
            PressureAccessChecks.run(server.overworld());
            TransportUpgradeChecks.run(server.overworld());
            var compression = Ic2Recipes.find(Process.COMPRESSION, new ItemStack(Items.COBBLED_DEEPSLATE)).orElseThrow();
            require(compression.id().equals(ResourceLocation.parse("exampleaddon:deepslate")) && compression.output() == Items.DEEPSLATE, "addon compression is used by the machine recipe lookup");
            var extraction = Ic2Recipes.find(Process.EXTRACTION, new ItemStack(Items.MOSS_BLOCK)).orElseThrow();
            require(extraction.output() == Items.SLIME_BALL && extraction.outputCount() == 2, "addon extraction keeps output count");
            require(Ic2Recipes.find(Process.COMPRESSION, new ItemStack(Items.COPPER_INGOT)).orElseThrow().inputCount() == 8, "builtin recipe survives addon registration");
            require(Ic2Api.item(ResourceLocation.parse("ic2:missing_api_test_item")).isEmpty(), "unknown registry ID does not resolve to air");
            try {
                Ic2Recipes.addExtraction(ResourceLocation.parse("exampleaddon:late"), Items.NETHERRACK, Items.SLIME_BALL, 1);
                throw new AssertionError("late registration was accepted");
            } catch (IllegalStateException expected) { /* registry is correctly frozen */ }
            var level = server.overworld();
            var distant = new BlockPos(10_000_000, 300, 10_000_000);
            require(!level.hasChunkAt(distant), "fixture distant chunk must be unloaded");
            require(Ic2Api.machineInfo(level, distant).isEmpty() && !level.hasChunkAt(distant), "query never loads a chunk");
            var pos = new BlockPos(0, 300, 0);
            level.getChunkAt(pos); // Explicit test-fixture loading, outside the API query.
            require(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()), "fixture positions must be empty");
            try {
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:plasmafier")).orElseThrow().defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Ic2Api.block(ResourceLocation.parse("ic2:generator")).orElseThrow().defaultBlockState());
                var before = Ic2Api.machineInfo(level, pos).orElseThrow();
                require(before.energyStored() == 0 && before.energyCapacity() == 5120000 && before.energyUnit() == MachineSnapshot.EnergyUnit.EU, "snapshot reports actual machine capacity and unit");
                int spent = EuNetwork.send(level, pos.above(), 128, Set.of(Direction.DOWN), 0);
                require(spent == 128 && Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 128, "public EU API transfers energy into a real IC2 machine");
                require(before.energyStored() == 0, "old snapshot stays unchanged");
                try { before.values().put("mutation", 1L); throw new AssertionError("mutable snapshot"); }
                catch (UnsupportedOperationException expected) { /* immutable snapshot */ }
            } finally {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            }
            verifyGenerators(level, pos);
            ChemistrySmokeChecks.verify(level, pos);
            ChargingSmokeChecks.verify(level, pos);
            NuclearGenerationSmokeChecks.verify(level);
            StructureChunkChecks.start(server);
        } catch (Throwable error) {
            LOGGER.error("IC2 API runtime smoke checks FAILED", error);
            server.halt(false);
        }
    }
    private static void verifyGenerators(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var lookup = level.registryAccess();
        try {
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:windmill")).orElseThrow().defaultBlockState());
            var rotorSlot = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            var rotor = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:wooden_rotor")).orElseThrow());
            require(rotorSlot != null && rotorSlot.insertItem(0, rotor, false).isEmpty(), "windmill accepts a rotor through automation");
            tick(level, pos, level.getBlockEntity(pos).getType());
            var wind = Ic2Api.machineInfo(level, pos).orElseThrow();
            require(wind.energyCapacity() == 4000 && wind.values().get("free_air_permille") > 0, "windmill samples connected air without a client");
            require(rotorSlot.extractItem(0, 1, false).getItem() == rotor.getItem(), "rotor can be replaced through automation");
            tick(level, pos, level.getBlockEntity(pos).getType());
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("speed_thousandths") == 0, "removing rotor stops windmill");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            String[] fluids = {"woodgas", "bio_fuel", "alcohol"};
            int[] output = {10, 128, 50};
            for (int index = 0; index < fluids.length; index++) {
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:liquid_fuel_generator")).orElseThrow().defaultBlockState());
                var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
                var profile = Ic2Api.fluid(ResourceLocation.parse("ic2:" + fluids[index])).orElseThrow();
                require(tank != null && tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "generator rejects water");
                require(tank.fill(new net.neoforged.neoforge.fluids.FluidStack(profile, 16000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 16000, "generator accepts its liquid fuel");
                tick(level, pos, level.getBlockEntity(pos).getType());
                var before = Ic2Api.machineInfo(level, pos).orElseThrow();
                require(before.energyStored() == output[index] && before.values().get("fuel_ticks") > 0, "fuel converts to the configured EU rate");
                if (fluids[index].equals("alcohol")) {
                    for (int count = 0; count < 199; count++) tick(level, pos, level.getBlockEntity(pos).getType());
                    before = Ic2Api.machineInfo(level, pos).orElseThrow();
                    require(before.energyStored() == 10000, "generator reaches full EU buffer");
                    for (int count = 0; count < 10; count++) tick(level, pos, level.getBlockEntity(pos).getType());
                    require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("fuel_ticks").equals(before.values().get("fuel_ticks")), "full EU buffer pauses real burning fuel");
                }
                var saved = level.getBlockEntity(pos).saveWithFullMetadata(lookup);
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:liquid_fuel_generator")).orElseThrow().defaultBlockState());
                level.getBlockEntity(pos).loadWithComponents(saved, lookup);
                var restored = Ic2Api.machineInfo(level, pos).orElseThrow();
                require(restored.energyStored() == before.energyStored() && restored.values().get("fuel_ticks").equals(before.values().get("fuel_ticks")), "EU and burning fuel survive reload");
                require(restored.values().get("fuel_mb").equals(before.values().get("fuel_mb")) && restored.values().get("fuel_mb") == 15000, "liquid tank survives reload");
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:liquid_fuel_generator")).orElseThrow().defaultBlockState());
            var slots = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            var cell = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:cell_woodgas")).orElseThrow());
            require(slots != null && slots.insertItem(0, cell, false).isEmpty(), "fuel cell enters the side input");
            tick(level, pos, level.getBlockEntity(pos).getType());
            require(slots.getStackInSlot(1).is(Ic2Api.item(ResourceLocation.parse("ic2:cell_empty")).orElseThrow()), "fuel cell returns an empty cell");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().values().get("fuel_mb") == 1000, "container transfer preserves fluid amount");
        } finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
    }
    static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(net.minecraft.server.level.ServerLevel level,
            BlockPos pos, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        var entity = type.getBlockEntity(level, pos);
        if (entity == null || !(entity.getBlockState().getBlock() instanceof net.minecraft.world.level.block.EntityBlock block))
            throw new AssertionError("Missing machine ticker fixture");
        var ticker = block.getTicker(level, entity.getBlockState(), type);
        if (ticker == null) throw new AssertionError("Missing server ticker");
        ticker.tick(level, pos, entity.getBlockState(), entity);
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
