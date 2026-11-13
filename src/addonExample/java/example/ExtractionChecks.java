package example;

import ic2.api.Ic2Api;
import ic2.api.energy.EnergySink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;

final class ExtractionChecks {
    static void run(ServerLevel level) {
        var pos = new BlockPos(472, 300, 472);
        level.getChunkAt(pos);
        try {
            var block = Ic2Api.block(ResourceLocation.parse("ic2:overclocked_pump"));
            require(block.isPresent(), "source accelerated pump is registered for the fisher acquisition chain");
            level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState());
            var machine = level.getBlockEntity(pos);
            var menu = ((MenuProvider) machine).createMenu(0, net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level).getInventory(), net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level));
            require(menu.slots.get(1).safeInsert(new ItemStack(Items.BUCKET)).isEmpty(), "accelerated pump accepts a container in source slot one");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyCapacity() == 2500, "source accelerated pump capacity");
            require(((EnergySink) machine).canReceiveEnergy(Direction.UP, 0, 128) && !((EnergySink) machine).canReceiveEnergy(Direction.UP, 0, 512), "source accelerated pump MV input");
            ((EnergySink) machine).receiveEnergy(200);
            for (int tick = 0; tick < 12; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            var saved = machine.saveWithoutMetadata(level.registryAccess());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos, block.orElseThrow().defaultBlockState());
            machine = level.getBlockEntity(pos); machine.loadWithComponents(saved, level.registryAccess());
            menu = ((MenuProvider) machine).createMenu(0, net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level).getInventory(), net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level));
            for (int tick = 12; tick < 25; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            require(menu.slots.get(2).getItem().is(Items.WATER_BUCKET) && menu.slots.get(1).getItem().isEmpty(), "accelerated pump preserves progress and fills one bucket in 25 ticks");
            require(Ic2Api.machineInfo(level, pos).orElseThrow().energyStored() == 0 && level.getBlockState(pos.below()).getFluidState().isEmpty(), "accelerated pumping spends exactly 200 EU and removes one source");
            var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
            require(tank != null && tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "pump tank is extraction-only");
            ((EnergySink) machine).receiveEnergy(200);
            for (int tick = 0; tick < 25; tick++) ApiSmokeChecks.tick(level, pos, machine.getType());
            var source = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
            var external = (ic2.api.machine.FluidPump) machine;
            require(external.pumpFluid(source, true) && tank.getFluidInTank(0).isEmpty(), "miner handoff simulation leaves prepared progress and tank untouched");
            require(external.pumpFluid(source, false) && tank.getFluidInTank(0).getAmount() == 1000 && !external.pumpFluid(source, false), "prepared pump accepts exactly one external bucket then requires another charged cycle");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            require(tank.getTanks() == 0 && !external.pumpFluid(source, false), "cached pump references stop after removal");
            for (String name : java.util.List.of("electric_fisher", "electric_enchanter", "chunkloader", "magnetizer", "tesla_coil", "colossal_base", "colossal_furnace", "colossal_macerator", "colossal_extractor", "colossal_compressor", "colossal_recycler", "overclocked_pump")) {
                var state = Ic2Api.block(ResourceLocation.parse("ic2:" + name)).orElseThrow().defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                var drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, level.getBlockEntity(pos));
                require(drops.stream().anyMatch(item -> item.is(state.getBlock().asItem())), name + " has a working survival block drop");
            }
        } finally { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState()); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
