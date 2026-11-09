package ic2.core.machine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

final class HeatPumpMenu extends AbstractContainerMenu {
    private final ItemStack pump;
    private final InteractionHand hand;
    private final DataSlot directions;

    HeatPumpMenu(int id, Inventory inventory) {
        this(id, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }

    HeatPumpMenu(int id, ItemStack pump, InteractionHand hand) {
        super(Machines.HEAT_PUMP_MENU.get(), id);
        this.pump = pump;
        this.hand = hand;
        directions = addDataSlot(pump.isEmpty() ? DataSlot.standalone() : new DataSlot() {
            @Override public int get() { return HeatPumpItem.directions(pump); }
            @Override public void set(int value) {}
        });
    }

    boolean pulls(int direction) {
        return (directions.get() & (1 << direction)) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= 4 || !stillValid(player)) {
            return false;
        }
        HeatPumpItem.toggle(pump, id);
        player.getInventory().setChanged();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || pump.getItem() instanceof HeatPumpItem && player.getItemInHand(hand) == pump;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
