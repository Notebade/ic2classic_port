package ic2.core.machine;

import ic2.api.energy.EnergyConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

final class MonitorBlock extends Block implements EntityBlock, EnergyConnectable {

    MonitorBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH).setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new MonitorEntity(p, s);
    }

    @Override
    public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return !l.isClientSide && t == NuclearInfrastructure.MONITOR_ENTITY.get() ? (world,p,state,e)->((MonitorEntity)e).tick() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (!l.isClientSide && l.getBlockEntity(p) instanceof MonitorEntity m) player.openMenu(m);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    protected void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moved) {
        if (!s.is(next.getBlock()) && !l.isClientSide && l.getBlockEntity(p) instanceof MonitorEntity m) for (int i = 0; i < m.cards.getSlots(); i++) net.minecraft.world.Containers.dropItemStack(l, p.getX(), p.getY(), p.getZ(), m.cards.getStackInSlot(i));
        super.onRemove(s, l, p, next, moved);
    }
}
