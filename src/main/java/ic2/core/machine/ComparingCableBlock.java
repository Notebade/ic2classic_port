package ic2.core.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class ComparingCableBlock extends EnergyMeterCableBlock implements EntityBlock {
    private static final MapCodec<ComparingCableBlock> CODEC = simpleCodec(ComparingCableBlock::new);

    ComparingCableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWER, 0));
    }

    @Override
    protected MapCodec<ComparingCableBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWER);
    }

    @Override
    public ComparingCableEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComparingCableEntity(pos, state);
    }

    @Override
    void recordTransfer(Level level, BlockPos pos, int energy) {
        if (level.getBlockEntity(pos) instanceof ComparingCableEntity cable && cable.recordTransfer(energy)) {
            scheduleSample(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ComparingCableEntity cable) {
            int power = cable.sample();
            if (state.getValue(BlockStateProperties.POWER) != power) {
                level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.POWER, power));
            }
            if (power > 0) {
                level.scheduleTick(pos, this, SAMPLE_INTERVAL);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ComparingCableEntity cable) {
            player.openMenu(cable);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(BlockStateProperties.POWER);
    }
}
