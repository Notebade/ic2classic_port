package ic2.core.tree;

import ic2.core.IC2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

final class RubberLogBlock extends RotatedPillarBlock {
    static final BooleanProperty RESIN = BooleanProperty.create("resin");
    static final BooleanProperty COLLECTABLE = BooleanProperty.create("collectable");
    static final DirectionProperty RESIN_FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int REFILL_INTERVAL = 100;
    private static final int REFILL_CHANCE = 200;
    private static final int DRY_TAP_CHANCE = 5;

    RubberLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RESIN, false).setValue(COLLECTABLE, false)
            .setValue(RESIN_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN, COLLECTABLE, RESIN_FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(RESIN_FACING, rotation.rotate(state.getValue(RESIN_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(RESIN_FACING, mirror.mirror(state.getValue(RESIN_FACING)));
    }

    static boolean canTap(BlockState state, Direction face) {
        return state.getBlock() instanceof RubberLogBlock && state.getValue(AXIS) == Direction.Axis.Y
            && state.getValue(RESIN) && state.getValue(RESIN_FACING) == face;
    }

    void tap(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        if (state.getValue(COLLECTABLE)) {
            level.setBlockAndUpdate(pos, state.setValue(COLLECTABLE, false));
            level.scheduleTick(pos, this, REFILL_INTERVAL);
            dropResin(level, pos, face);
            return;
        }
        if (level.random.nextInt(DRY_TAP_CHANCE) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(RESIN, false));
        }
        if (level.random.nextInt(DRY_TAP_CHANCE) == 0) {
            dropResin(level, pos, face);
        }
    }

    private static void dropResin(ServerLevel level, BlockPos pos, Direction face) {
        popResourceFromFace(level, pos, face, new ItemStack(IC2.STICKY_RESIN.get(), 1 + level.random.nextInt(3)));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(RESIN) || state.getValue(COLLECTABLE)) {
            return;
        }
        if (random.nextInt(REFILL_CHANCE) == 0) {
            level.setBlockAndUpdate(pos, state.setValue(COLLECTABLE, true));
        } else {
            level.scheduleTick(pos, this, REFILL_INTERVAL);
        }
    }
}
