package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class EnvironmentalGeneratorBlock extends ActiveMachineBlock {
    EnvironmentalGeneratorBlock(Properties properties, MachineKind kind) {
        super(properties, kind);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineEntity machine) {
            player.displayClientMessage(Component.translatable(
                "message.ic2.environmental_generator_output", getName(), machine.environmentalProduction()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
