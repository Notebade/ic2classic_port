package ic2.core.equipment;

import ic2.core.machine.PortableReactor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NuclearJetpackMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    static final java.util.function.Supplier<MenuType<NuclearJetpackMenu>> TYPE = MENUS.register("nuclear_jetpack", () ->
        net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inventory, buffer) -> {
            var hand = buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            return new NuclearJetpackMenu(id, inventory, new SimpleContainer(25), inventory.player.getItemInHand(hand), hand);
        }));
    private final ItemStack owner;
    private final InteractionHand hand;
    static void register(net.neoforged.bus.api.IEventBus bus) { MENUS.register(bus); }
    static void open(Player player, ItemStack owner, InteractionHand hand, int moduleIndex) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
        var pack = moduleIndex < 0 ? owner : ArmorModules.read(owner).get(moduleIndex).stack();
        var items = pack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        var contents = new SimpleContainer(25);
        for (int i = 0; i < Math.min(items.getSlots(), 25); i++) contents.setItem(i, items.getStackInSlot(i));
        contents.addListener(changed -> {
            var inventory = ItemContainerContents.fromItems(java.util.stream.IntStream.range(0, 25).mapToObj(contents::getItem).toList());
            if (moduleIndex < 0) owner.set(DataComponents.CONTAINER, inventory);
            else {
                var modules = new java.util.ArrayList<>(ArmorModules.read(owner));
                if (moduleIndex >= modules.size()) return;
                var previous = modules.get(moduleIndex); var module = previous.stack();
                module.set(DataComponents.CONTAINER, inventory);
                modules.set(moduleIndex, new ic2.api.item.InstalledArmorModule(previous.category(), module));
                ArmorModules.write(owner, modules, ArmorModules.derive(owner, modules, ArmorModules.configuration(owner).enhanced()));
            }
        });
        server.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new NuclearJetpackMenu(id, inventory, contents, owner, hand),
            pack.getHoverName()), buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
    }
    private NuclearJetpackMenu(int id, Inventory inventory, Container contents, ItemStack owner, InteractionHand hand) {
        super(TYPE.get(), id); this.owner = owner; this.hand = hand;
        for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) addSlot(new Slot(contents, x + y * 5, 44 + x * 18, 18 + y * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return PortableReactor.accepts(stack); }
        });
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 140 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x, 8 + x * 18, 198));
    }
    @Override public boolean stillValid(Player player) { return player.getItemInHand(hand) == owner; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem();
        if (stack.isEmpty() || stack == owner) return ItemStack.EMPTY;
        var result = stack.copy();
        if (!(index < 25 ? moveItemStackTo(stack, 25, slots.size(), true) : moveItemStackTo(stack, 0, 25, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return result;
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.getItem() != owner && super.canTakeItemForPickAll(stack, slot); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < slots.size() && slots.get(slot).getItem() == owner
            || type == ClickType.SWAP && (button == 40 && hand == InteractionHand.OFF_HAND
                || hand == InteractionHand.MAIN_HAND && button == player.getInventory().selected)) return;
        super.clicked(slot, button, type, player);
    }
}
