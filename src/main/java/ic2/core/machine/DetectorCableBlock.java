package ic2.core.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

final class DetectorCableBlock extends EnergyCableBlock {
    private static final MapCodec<DetectorCableBlock> CODEC = simpleCodec(DetectorCableBlock::new);
    private static final BooleanProperty TRANSFERRED = BooleanProperty.create("transferred");
    private static final int SAMPLE_INTERVAL = 20;

    DetectorCableBlock(Properties properties) {
        super(properties, 500, 2048, 4);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWERED, false)
            .setValue(TRANSFERRED, false));
    }

    @Override
    protected MapCodec<DetectorCableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED, TRANSFERRED);
    }

    static void recordTransfer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DetectorCableBlock detector && !state.getValue(TRANSFERRED)) {
            level.setBlock(pos, state.setValue(TRANSFERRED, true), Block.UPDATE_KNOWN_SHAPE);
            int delay = SAMPLE_INTERVAL - Math.floorMod(level.getGameTime() + pos.hashCode(), SAMPLE_INTERVAL);
            level.scheduleTick(pos, detector, delay);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean transferred = state.getValue(TRANSFERRED);
        boolean powered = state.getValue(BlockStateProperties.POWERED);
        BlockState sampled = state.setValue(TRANSFERRED, false).setValue(BlockStateProperties.POWERED, transferred);
        level.setBlock(pos, sampled, powered == transferred ? Block.UPDATE_KNOWN_SHAPE : Block.UPDATE_ALL);
        if (transferred) {
            level.scheduleTick(pos, this, SAMPLE_INTERVAL);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }
}
