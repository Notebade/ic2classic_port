package ic2.core.machine;

import ic2.api.energy.EnergyConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

final class PoweredLampBlock extends Block implements EntityBlock, EnergyConnectable {
    static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    final boolean adjustable;

    PoweredLampBlock(Properties p, boolean adjustable) {
        super(p);
        this.adjustable = adjustable;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIGHT, net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, context.getClickedFace());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new PoweredLampEntity(p, s);
    }

    @Override
    public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return !l.isClientSide && t == NuclearInfrastructure.LAMP_ENTITY.get() ? (world,p,state,e)->((PoweredLampEntity)e).tick() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
        if (!adjustable) return InteractionResult.PASS;
        if (!l.isClientSide && l.getBlockEntity(p) instanceof PoweredLampEntity lamp) lamp.adjust(player.isShiftKeyDown() ? -1 : 1);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
