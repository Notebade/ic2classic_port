package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

class SolarPanelBlock extends MachineBlock {
    static final int BASE_OUTPUT = 1;
    static final int LV_OUTPUT = 8;
    static final int MV_OUTPUT = 64;
    private final int output;

    SolarPanelBlock(Properties properties, int output) {
        super(properties, MachineKind.SOLAR_PANEL);
        this.output = output;
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }

    static boolean hasSun(Level level, BlockPos panel) {
        if (!level.dimensionType().hasSkyLight() || !level.isDay()) {
            return false;
        }
        BlockPos above = panel.above();
        BlockState cover = level.getBlockState(above);
        // The below-water sky check skips the block at its lower endpoint.
        return (cover.getBlock() instanceof LiquidBlock || cover.getLightBlock(level, above) == 0)
            && level.canSeeSkyFromBelowWater(above)
            && (!level.getBiome(above).value().hasPrecipitation() || (!level.isRaining() && !level.isThundering()));
    }

    boolean emitsTo(Direction side) {
        return side != Direction.UP;
    }

    int output() {
        return output;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int production = state.getValue(BlockStateProperties.LIT) ? output : 0;
            player.displayClientMessage(Component.translatable("message.ic2.solar_panel_output", production), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
