package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;

final class ThermalGeneratorBlock extends FluidGeneratorBlock {
    static final BooleanProperty PASSIVE = BooleanProperty.create("passive");
    private static final int HEAT_RADIUS = 7;

    ThermalGeneratorBlock(Properties properties) {
        super(properties, MachineKind.THERMAL_GENERATOR, Fluids.LAVA, ThermalProduction.TANK_CAPACITY);
        registerDefaultState(defaultBlockState().setValue(PASSIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PASSIVE);
    }

    static int sampleHeat(Level level, BlockPos origin) {
        int radius = HEAT_RADIUS;
        int heat = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (level.isLoaded(pos)) {
                heat += heatValue(level.getBlockState(pos));
            }
        }
        return ThermalProduction.heatPoints(heat, level.getBiome(origin).value().getBaseTemperature());
    }

    private static int heatValue(BlockState state) {
        var fluid = state.getFluidState().getType();
        if (fluid == Fluids.FLOWING_LAVA) {
            return 3;
        }
        if (fluid == Fluids.LAVA || state.is(BlockTags.FIRE)) {
            return 10;
        }
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return 7;
        }
        if (state.is(Blocks.OBSIDIAN)) {
            return 5;
        }
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return -3;
        }
        if (state.is(BlockTags.SNOW) || state.is(Blocks.POWDER_SNOW)) {
            return -5;
        }
        if (state.is(BlockTags.ICE)) {
            return -10;
        }
        // Modern Minecraft no longer exposes the old stone and metal materials.
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? 2 : 0;
    }
}
