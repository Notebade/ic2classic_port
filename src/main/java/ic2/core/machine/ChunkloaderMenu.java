package ic2.core.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

final class ChunkloaderMenu extends AbstractContainerMenu {
    private final ChunkloaderEntity device;
    private final ContainerData data;
    ChunkloaderMenu(int id, Inventory inventory) { this(id, inventory, null, new SimpleContainerData(6)); }
    ChunkloaderMenu(int id, Inventory inventory, ChunkloaderEntity device, ContainerData data) {
        super(Chunkloading.MENU.get(), id); this.device = device; this.data = data; addDataSlots(data);
    }
    int value(int index) { return index == 0 ? data.get(index) & 65535 : data.get(index); }
    @Override public boolean stillValid(Player player) { return device == null || !device.isRemoved() && device.getLevel().getBlockEntity(device.getBlockPos()) == device
        && player.distanceToSqr(device.getBlockPos().getCenter()) <= 64 && device.allowed(player, false); }
    @Override public boolean clickMenuButton(Player player, int button) { if (device == null || !stillValid(player)) return false; device.button(player, button); return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
