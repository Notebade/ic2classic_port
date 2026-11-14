package ic2.core.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

final class TapCollector extends Block {
    private static final IntegerProperty FILL = IntegerProperty.create("fill_stage", 0, 5);
    private static final int INTERVAL = 200;

    TapCollector(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILL, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, FILL);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (var side : Direction.Plane.HORIZONTAL) {
            var pos = context.getClickedPos();
            if (RubberLogBlock.canTap(context.getLevel().getBlockState(pos.relative(side)), side.getOpposite())) {
                return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, side);
            }
        }
        return null;
    }

    private static BlockPos source(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return RubberLogBlock.canTap(level.getBlockState(source(state, pos)),
            state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!old.is(this)) level.scheduleTick(pos, this, INTERVAL);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && !canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        int fill = state.getValue(FILL);
        if (fill == 5) return;
        var logPos = source(state, pos);
        var log = level.getBlockState(logPos);
        if (log.getValue(RubberLogBlock.COLLECTABLE)) {
            level.setBlockAndUpdate(logPos, log.setValue(RubberLogBlock.COLLECTABLE, false));
            level.setBlockAndUpdate(pos, state.setValue(FILL, Math.min(5, fill + 1 + random.nextInt(3))));
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    @Override protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder context) {
        var drops = new java.util.ArrayList<>(super.getDrops(state, context));
        int fill = state.getValue(FILL);
        if (fill > 0) drops.add(new ItemStack(ic2.core.IC2.STICKY_RESIN.get(), fill));
        return drops;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int fill = state.getValue(FILL);
        if (fill == 0) return InteractionResult.PASS;
        if (!level.isClientSide && level.setBlockAndUpdate(pos, state.setValue(FILL, 0))) {
            popResource(level, pos, new ItemStack(ic2.core.IC2.STICKY_RESIN.get(), fill));
            level.scheduleTick(pos, this, INTERVAL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
