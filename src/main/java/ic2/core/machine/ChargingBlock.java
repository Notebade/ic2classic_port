package ic2.core.machine;

import ic2.api.energy.EnergyConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

class ChargingBlock extends Block implements EntityBlock, EnergyConnectable {
    final ChargingDevices.Kind kind;
    ChargingBlock(Properties properties, ChargingDevices.Kind kind) {
        super(properties); this.kind = kind; registerDefaultState(defaultBlockState().setValue(BlockStateProperties.LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.FACING, BlockStateProperties.LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind.pad()) return defaultBlockState();
        Direction direction = context.getNearestLookingDirection().getOpposite();
        if (kind == ChargingDevices.Kind.SLAVE) {
            direction = null;
            for (var side : Direction.values()) if (canFace(context.getLevel(), context.getClickedPos(), side)) { direction = side; break; }
            if (direction == null) return null;
        }
        return defaultBlockState().setValue(BlockStateProperties.FACING, direction);
    }
    boolean canFace(Level level, BlockPos pos, Direction direction) {
        if (kind != ChargingDevices.Kind.SLAVE) return true;
        BlockPos target = pos.relative(direction);
        return level.hasChunkAt(target) && level.getBlockEntity(target) instanceof ChargingEntity device
            && device.kind == ChargingDevices.Kind.MASTER && device.front() != direction.getOpposite();
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return kind.pad() ? state : state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return kind.pad() ? state : rotate(state, mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbor, boolean moving) {
        if (!level.isClientSide && kind == ChargingDevices.Kind.SLAVE) {
            Direction front = state.getValue(BlockStateProperties.FACING);
            if (level.hasChunkAt(pos.relative(front)) && !canFace(level, pos, front)) {
                Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(this)); level.removeBlock(pos, false); return;
            }
        }
        super.neighborChanged(state, level, pos, block, neighbor, moving);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ChargingEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ChargingDevices.ENTITY.get() ? (world, pos, current, entity) -> ((ChargingEntity) entity).tick() : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ChargingEntity device) player.openMenu(device);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ChargingEntity device) device.dropContents();
        super.onRemove(state, level, pos, replacement, moved);
    }
}
