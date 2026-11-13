package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

final class PowerAccessChecks {
    static void run(ServerLevel level) {
        var origin = new BlockPos(580, 300, 580); var placed = new java.util.ArrayList<BlockPos>(); level.getChunkAt(origin);
        var partBlock = Ic2Api.block(ResourceLocation.parse("ic2:fuel_boiler_structure")).orElseThrow();
        try {
            for (int i = 0; i < 8; i++) {
                var pos = origin.offset(-(i % 2), -(i / 4), i / 2 % 2); placed.add(pos);
                var state = (i == 0 ? Ic2Api.block(ResourceLocation.parse("ic2:fuel_boiler")).orElseThrow() : partBlock).defaultBlockState();
                if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
                level.setBlock(pos, state, 2 | 16);
            }
            var controller = level.getBlockEntity(origin); ApiSmokeChecks.tick(level, origin, controller.getType());
            require(Ic2Api.machineInfo(level, origin.below()).isPresent(), "actual boiler ticker binds its structure");
            var replaced = origin.offset(-1, -1, 1); level.setBlock(replaced, Blocks.AIR.defaultBlockState(), 2 | 16); level.setBlock(replaced, partBlock.defaultBlockState(), 2 | 16);
            require(Ic2Api.machineInfo(level, origin.below()).isEmpty(), "reading an existing linked part must not claim a newly replaced boiler part");
            require(!level.getBlockEntity(replaced).saveWithoutMetadata(level.registryAccess()).contains("controller"), "power structure API leaves replacement ownership untouched");
            var tank = level.getCapability(Capabilities.FluidHandler.BLOCK, origin, Direction.UP);
            var water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000);
            require(tank.fill(water, IFluidHandler.FluidAction.SIMULATE) == 0, "capability simulation cannot form a replacement structure");
            ApiSmokeChecks.tick(level, origin, controller.getType());
            require(Ic2Api.machineInfo(level, origin.below()).isPresent() && tank.fill(water, IFluidHandler.FluidAction.SIMULATE) == 1000
                && tank.getFluidInTank(0).isEmpty(), "actual ticker reforms; subsequent fluid simulation stays pure");
        } finally { for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
