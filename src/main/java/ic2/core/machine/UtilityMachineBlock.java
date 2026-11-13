package ic2.core.machine;

import ic2.api.energy.EnergyConnectable;
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

final class UtilityMachineBlock extends Block implements EntityBlock, EnergyConnectable {
    final boolean tesla;

    UtilityMachineBlock(Properties properties, boolean tesla) { super(properties); this.tesla = tesla; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new UtilityMachineEntity(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != UtilityMachines.ENTITY.get() ? null : (world, pos, block, entity) -> ((UtilityMachineEntity) entity).tick();
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!tesla && stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2", "upgrade_overclocker")))
                && level.getBlockEntity(pos) instanceof UtilityMachineEntity machine && !machine.upgraded) {
            if (!level.isClientSide) { machine.upgraded = true; machine.setChanged(); if (!player.isCreative()) stack.shrink(1); }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
