package ic2.core.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class SplitterCableBlock extends EnergyCableBlock {
    private static final MapCodec<SplitterCableBlock> CODEC = simpleCodec(SplitterCableBlock::new);

    SplitterCableBlock(Properties properties) {
        super(properties, 500, 2048, 4);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected MapCodec<SplitterCableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(BlockStateProperties.POWERED,
            context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) {
            updatePower(state, level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos,
                                   boolean moved) {
        updatePower(state, level, pos);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    boolean canCarry(BlockState state, int packetSize) {
        return !state.getValue(BlockStateProperties.POWERED) && super.canCarry(state, packetSize);
    }

    private static void updatePower(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide) {
            boolean powered = level.hasNeighborSignal(pos);
            if (state.getValue(BlockStateProperties.POWERED) != powered) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.POWERED, powered));
            }
        }
    }
}
