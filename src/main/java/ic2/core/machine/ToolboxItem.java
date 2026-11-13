package ic2.core.machine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

final class ToolboxItem extends Item {

    ToolboxItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var box = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var contents = new net.minecraft.world.SimpleContainer(9){

                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    return slot < 8 && stack.getItem() != ToolboxItem.this && stack.canFitInsideContainerItems();
                }

                @Override
                public void setChanged() {
                    box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.stream.IntStream.range(0, 9).mapToObj(this::getItem).toList()));
                    super.setChanged();
                }

                @Override
                public boolean stillValid(Player owner) {
                    return owner == player && player.getItemInHand(hand) == box;
                }
            };
            var saved = box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            var stacks = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
            saved.copyInto(stacks);
            for (int slot = 0; slot < 8; slot++) contents.setItem(slot, stacks.get(slot));
            player.openMenu(new SimpleMenuProvider((id,inventory,owner)->new ToolboxMenu(id, inventory, contents, box), getName(box)));
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }
}
