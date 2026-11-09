package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

final class ReactorChamberBlock extends Block {
    ReactorChamberBlock(Properties properties) {
        super(properties);
    }

    static MachineEntity reactor(Level level, BlockPos chamber) {
        boolean steam = level.getBlockState(chamber).is(Machines.STEAM_REACTOR_CHAMBER.get());
        Block core = steam ? Machines.STEAM_REACTOR.get() : Machines.NUCLEAR_REACTOR.get();
        for (Direction direction : Direction.values()) {
            BlockPos pos = chamber.relative(direction);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineEntity machine
                    && level.getBlockState(pos).is(core)) {
                return machine;
            }
        }
        return null;
    }

    static IFluidHandler steamFluid(Level level, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity entity, Direction side) {
        MachineEntity machine = reactor(level, pos);
        return machine == null ? null : machine.steamFluid(side);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ReactorCardItem card) {
            MachineEntity machine = reactor(level, pos);
            if (machine != null) {
                card.interact(player, stack, machine);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!level.isClientSide && state.is(Machines.STEAM_REACTOR_CHAMBER.get())
                && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
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
