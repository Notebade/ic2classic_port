package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class PressurePartBlock extends Block implements EntityBlock {
    PressurePartBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PressurePartEntity(pos, state); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PressurePartEntity part) {
            var controller = part.controller();
            if (controller == null) player.displayClientMessage(Component.translatable("message.ic2.pressure_incomplete"), true);
            else player.openMenu(controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
