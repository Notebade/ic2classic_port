package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

final class BatBoxBlock extends MachineBlock {
    BatBoxBlock(Properties properties) {
        super(properties, MachineKind.BATBOX);
    }

    @Override
    DirectionProperty facingProperty() {
        return BlockStateProperties.FACING;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return defaultBlockState();
        }
        int pitch = Math.round(player.getXRot());
        if (pitch >= 65) {
            return defaultBlockState().setValue(facingProperty(), Direction.UP);
        }
        if (pitch <= -65) {
            return defaultBlockState().setValue(facingProperty(), Direction.DOWN);
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            player.displayClientMessage(machine.storageStatus(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
