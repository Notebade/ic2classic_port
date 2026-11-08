package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class LvTransformerBlock extends DirectionalMachineBlock {
    static final int LOW_PACKET = 32;
    static final int HIGH_PACKET = 128;
    static final int LOW_PACKETS = HIGH_PACKET / LOW_PACKET;

    LvTransformerBlock(Properties properties) {
        super(properties, MachineKind.LV_TRANSFORMER);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            player.displayClientMessage(machine.transformerStatus(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
