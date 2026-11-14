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

final class ToolboxItem extends Item implements ic2.api.item.Toolbox {
    private static final net.minecraft.tags.TagKey<Item> TOOLS = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ic2", "toolbox_tools"));
    private final int capacity;

    ToolboxItem(Properties properties) {
        this(properties, 8);
    }
    ToolboxItem(Properties properties, int capacity) { super(properties.stacksTo(1)); this.capacity = capacity; }
    @Override public int slots() { return capacity; }
    @Override public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!stack.canFitInsideContainerItems() || stack.getItem() instanceof ic2.api.item.Toolbox || stack.getCount() > stack.getMaxStackSize()) return false;
        return stack.has(DataComponents.FOOD) || stack.is(TOOLS) || stack.getItem() instanceof ic2.api.item.BoxableItem boxable && boxable.canStoreInToolbox(stack)
            || stack.getItem() instanceof net.minecraft.world.item.TieredItem || stack.getItem() instanceof net.minecraft.world.item.ShearsItem
            || stack.getItem() instanceof net.minecraft.world.item.FishingRodItem || stack.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem
            || stack.getItem() instanceof net.minecraft.world.item.BrushItem || stack.getItem() instanceof ic2.api.item.ElectricItem;
    }
    private net.minecraft.core.NonNullList<ItemStack> contents(ItemStack box) {
        var stacks = net.minecraft.core.NonNullList.withSize(capacity, ItemStack.EMPTY);
        box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stacks);
        return stacks;
    }
    @Override public ItemStack stored(ItemStack box, int slot) { return box.is(this) && slot >= 0 && slot < capacity ? contents(box).get(slot) : ItemStack.EMPTY; }
    @Override public boolean store(ItemStack box, int slot, ItemStack item) {
        if (!box.is(this) || slot < 0 || slot >= capacity || !accepts(item)) return false;
        var stacks = contents(box); stacks.set(slot, item.copy()); box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks)); return true;
    }
    @Override public boolean scroll(ItemStack box, Player player, int direction) {
        if (capacity == 8 || direction == 0 || player.level().isClientSide) return false;
        boolean owned = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize()).anyMatch(slot -> player.getInventory().getItem(slot) == box);
        if (!owned) return false;
        var hand = player.getMainHandItem() == box ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var held = player.getItemInHand(hand);
        if (!accepts(held)) return false;
        var data = box.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        int selected = Math.floorMod(data.getInt("ic2_toolbox_selected"), capacity);
        for (int step = 1; step <= capacity; step++) {
            int slot = Math.floorMod(selected + Integer.signum(direction) * step, capacity);
            var next = stored(box, slot);
            if (next.isEmpty()) continue;
            if (!store(box, slot, held)) return false;
            player.setItemInHand(hand, next);
            data.putInt("ic2_toolbox_selected", slot); box.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
            player.getInventory().setChanged(); return true;
        }
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var box = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && capacity > 8) {
            if (!level.isClientSide) scroll(box, player, 1);
            return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
        }
        if (!level.isClientSide) {
            int physical = capacity == 8 ? 9 : capacity == 15 ? 18 : 45;
            var contents = new net.minecraft.world.SimpleContainer(physical){

                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    return slot < capacity && accepts(stack);
                }

                @Override
                public void setChanged() {
                    box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.stream.IntStream.range(0, capacity).mapToObj(this::getItem).toList()));
                    super.setChanged();
                }

                @Override
                public boolean stillValid(Player owner) {
                    return owner == player && player.getItemInHand(hand) == box;
                }
            };
            var saved = box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            var stacks = net.minecraft.core.NonNullList.withSize(physical, ItemStack.EMPTY);
            saved.copyInto(stacks);
            for (int slot = 0; slot < capacity; slot++) contents.setItem(slot, stacks.get(slot));
            player.openMenu(new SimpleMenuProvider((id,inventory,owner)->new ToolboxMenu(id, inventory, contents, box, capacity), getName(box)));
        }
        return InteractionResultHolder.sidedSuccess(box, level.isClientSide);
    }
}
