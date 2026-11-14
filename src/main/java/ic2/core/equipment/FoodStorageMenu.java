package ic2.core.equipment;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

final class FoodStorageMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    static final java.util.function.Supplier<MenuType<FoodStorageMenu>> TYPE = MENUS.register("food_storage", () ->
        net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inventory, buffer) -> {
            var hand = buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            return new FoodStorageMenu(id, inventory, new SimpleContainer(4), inventory.player.getItemInHand(hand), hand);
        }));
    private final ItemStack owner;
    private final InteractionHand hand;
    static void register(net.neoforged.bus.api.IEventBus bus) { MENUS.register(bus); }
    FoodStorageMenu(int id, Inventory inventory, Container contents, ItemStack owner, InteractionHand hand) {
        super(TYPE.get(), id); this.owner = owner; this.hand = hand;
        int[] x = {44, 62, 98, 116};
        for (int index = 0; index < 4; index++) addSlot(new Slot(contents, index, x[index], 24) {
            @Override public boolean mayPlace(ItemStack stack) {
                return getContainerSlot() < 2 ? stack.getItem() instanceof FoodCans
                    : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ic2.api.item.ArmorModule.id("tin_can"));
            }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 67 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 125));
    }
    @Override public boolean stillValid(Player player) { return player.getItemInHand(hand) == owner && FoodStorageModule.enabled(owner); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem();
        if (stack.isEmpty() || stack == owner) return ItemStack.EMPTY;
        var previous = stack.copy();
        if (!(index < 4 ? moveItemStackTo(stack, 4, slots.size(), true) : moveItemStackTo(stack, 0, 4, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return previous;
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.getItem() != owner && super.canTakeItemForPickAll(stack, slot); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < slots.size() && slots.get(slot).getItem() == owner
            || type == ClickType.SWAP && (button == 40 && hand == InteractionHand.OFF_HAND
                || hand == InteractionHand.MAIN_HAND && button == player.getInventory().selected)) return;
        super.clicked(slot, button, type, player);
    }
}
