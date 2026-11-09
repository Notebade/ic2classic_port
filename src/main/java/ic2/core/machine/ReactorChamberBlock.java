package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class ReactorChamberBlock extends Block {
    ReactorChamberBlock(Properties properties) {
        super(properties);
    }

    static MachineEntity reactor(Level level, BlockPos chamber) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = chamber.relative(direction);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineEntity machine
                    && level.getBlockState(pos).is(Machines.NUCLEAR_REACTOR.get())) {
                return machine;
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        MachineEntity machine = reactor(level, pos);
        if (machine == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.openMenu(machine);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
