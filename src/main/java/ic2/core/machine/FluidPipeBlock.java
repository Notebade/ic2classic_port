package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

final class FluidPipeBlock extends net.minecraft.world.level.block.PipeBlock implements EntityBlock {
    final boolean pressure;

    FluidPipeBlock(Properties p, boolean pressure) {
        super(0.25F, p);
        this.pressure = pressure;
        var state = defaultBlockState();
        for (var property : PROPERTY_BY_DIRECTION.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends FluidPipeBlock> codec() {
        return simpleCodec((p)->new FluidPipeBlock(p, pressure));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    private boolean connects(net.minecraft.world.level.LevelAccessor world, BlockPos pos, net.minecraft.core.Direction side) {
        if (world instanceof Level level && level.hasChunkAt(pos)) return level.getBlockState(pos).getBlock() instanceof FluidPipeBlock || level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, side.getOpposite()) != null;
        return false;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var state = defaultBlockState();
        for (var side : net.minecraft.core.Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(context.getLevel(), context.getClickedPos().relative(side), side));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction side, BlockState neighbor, net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockPos next) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(world, next, side));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new FluidPipeEntity(p, s);
    }

    @Override
    public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return !l.isClientSide && t == NuclearInfrastructure.PIPE_ENTITY.get() ? (world,p,state,e)->((FluidPipeEntity)e).tick() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        return !l.isClientSide && FluidUtil.interactWithFluidHandler(player, hand, l, p, hit.getDirection()) ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
