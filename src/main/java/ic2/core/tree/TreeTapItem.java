package ic2.core.tree;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

final class TreeTapItem extends Item {
    TreeTapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var face = context.getClickedFace();
        if (!RubberLogBlock.canTap(state, face)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && context.getPlayer() != null) {
            ((RubberLogBlock) state.getBlock()).tap(server, pos, state, face, context.getItemInHand());
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
