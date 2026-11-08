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

final class DetectorCableBlock extends EnergyMeterCableBlock {
    private static final MapCodec<DetectorCableBlock> CODEC = simpleCodec(DetectorCableBlock::new);
    private static final BooleanProperty TRANSFERRED = BooleanProperty.create("transferred");

    DetectorCableBlock(Properties properties) {
        super(properties);
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

    @Override
    void recordTransfer(Level level, BlockPos pos, int energy) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this) && !state.getValue(TRANSFERRED)) {
            level.setBlock(pos, state.setValue(TRANSFERRED, true), Block.UPDATE_KNOWN_SHAPE);
            scheduleSample(level, pos);
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
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }
}
