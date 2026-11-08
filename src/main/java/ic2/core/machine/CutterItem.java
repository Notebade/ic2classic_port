package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.registries.DeferredBlock;

final class CutterItem extends Item {
    private static final List<List<DeferredBlock<EnergyCableBlock>>> INSULATION_LEVELS = List.of(
        List.of(Machines.COPPER_CABLE, Machines.INSULATED_COPPER_CABLE),
        List.of(Machines.GOLD_CABLE, Machines.INSULATED_GOLD_CABLE, Machines.DOUBLE_INSULATED_GOLD_CABLE),
        List.of(Machines.IRON_CABLE, Machines.INSULATED_IRON_CABLE,
            Machines.DOUBLE_INSULATED_IRON_CABLE, Machines.TRIPLE_INSULATED_IRON_CABLE)
    );

    CutterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }
        var state = level.getBlockState(pos);
        var insulated = adjacentLevel(state.getBlock(), 1);
        if (insulated == null) {
            return InteractionResult.PASS;
        }
        var inventory = player.getInventory();
        ItemStack rubber = findRubber(inventory);
        if (rubber.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (!level.setBlockAndUpdate(pos, insulated.withPropertiesOf(state))) {
                return InteractionResult.PASS;
            }
            rubber.shrink(1);
            inventory.setChanged();
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static void onLeftClickBlock(LeftClickBlock event) {
        if (event.getAction() != LeftClickBlock.Action.START || !(event.getItemStack().getItem() instanceof CutterItem)) {
            return;
        }
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        // NeoForge posts this event before the game mode checks the block interaction range.
        if (!player.mayBuild() || !player.canInteractWithBlock(pos, 1.0) || !level.isInWorldBounds(pos)
            || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)) {
            return;
        }
        var state = level.getBlockState(pos);
        var stripped = adjacentLevel(state.getBlock(), -1);
        if (stripped == null) {
            return;
        }
        event.setCanceled(true);
        if (!level.isClientSide && level.setBlockAndUpdate(pos, stripped.withPropertiesOf(state))) {
            event.getItemStack().hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            Block.popResource(level, pos, new ItemStack(IC2.RUBBER.get()));
        }
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        int damage = stack.getDamageValue() + 1;
        if (damage >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copyWithCount(1);
        remaining.setDamageValue(damage);
        return remaining;
    }

    private static EnergyCableBlock adjacentLevel(Block block, int offset) {
        for (var variants : INSULATION_LEVELS) {
            for (int index = 0; index < variants.size(); index++) {
                if (variants.get(index).get() == block) {
                    int next = index + offset;
                    return next >= 0 && next < variants.size() ? variants.get(next).get() : null;
                }
            }
        }
        return null;
    }

    private static ItemStack findRubber(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(IC2.RUBBER.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
