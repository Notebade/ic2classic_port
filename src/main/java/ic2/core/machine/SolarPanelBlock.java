package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class SolarPanelBlock extends MachineBlock {
    static final int OUTPUT = 1;

    SolarPanelBlock(Properties properties) {
        super(properties, MachineKind.SOLAR_PANEL);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }

    static boolean hasSun(Level level, BlockPos panel) {
        BlockPos above = panel.above();
        return level.dimensionType().hasSkyLight() && level.isDay() && level.canSeeSkyFromBelowWater(above)
            && (!level.getBiome(above).value().hasPrecipitation() || (!level.isRaining() && !level.isThundering()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int production = state.getValue(BlockStateProperties.LIT) ? OUTPUT : 0;
            player.displayClientMessage(Component.translatable("message.ic2.solar_panel_output", production), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
