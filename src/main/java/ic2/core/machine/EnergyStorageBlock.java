package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class EnergyStorageBlock extends DirectionalMachineBlock {
    EnergyStorageBlock(Properties properties, MachineKind kind) {
        super(properties, kind);
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
