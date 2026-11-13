package ic2.core.tree;

import ic2.core.item.RechargeableItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

final class ElectricTreeTapItem extends RechargeableItem {
    private static final int ENERGY_PER_TAP = 50;

    ElectricTreeTapItem(Properties properties) {
        super(properties, 10_000, 100, 32);
    }

    @Override
    public boolean canProvideEnergy() {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var face = context.getClickedFace();
        var stack = context.getItemInHand();
        if (context.getPlayer() == null || !ic2.api.item.ElectricEquipment.canUse(stack, ENERGY_PER_TAP) || !RubberLogBlock.canTap(state, face)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            ((RubberLogBlock) state.getBlock()).tap(server, pos, state, face);
            ic2.api.item.ElectricEquipment.use(stack, ENERGY_PER_TAP, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
