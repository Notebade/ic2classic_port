package ic2.core.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

final class ComparingCableMenu extends AbstractContainerMenu {
    // One vanilla menu button packet carries both thresholds so the range changes atomically.
    private static final int THRESHOLD_BASE = ComparingCableEntity.MAX_THRESHOLD + 1;
    private final ComparingCableEntity cable;
    private final ContainerData thresholds;

    ComparingCableMenu(int id, Inventory inventory) {
        this(id, null, new SimpleContainerData(2));
    }

    ComparingCableMenu(int id, ComparingCableEntity cable, ContainerData thresholds) {
        super(Machines.COMPARING_CABLE_MENU.get(), id);
        this.cable = cable;
        this.thresholds = thresholds;
        addDataSlots(thresholds);
    }

    int minimum() {
        return thresholds.get(ComparingCableEntity.MINIMUM);
    }

    int maximum() {
        return thresholds.get(ComparingCableEntity.MAXIMUM);
    }

    static int thresholdButton(int minimum, int maximum) {
        return minimum * THRESHOLD_BASE + maximum;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return cable != null && buttonId >= 0 && stillValid(player)
            && cable.setThresholds(buttonId / THRESHOLD_BASE, buttonId % THRESHOLD_BASE);
    }

    @Override
    public boolean stillValid(Player player) {
        return cable == null || !cable.isRemoved() && player.level() == cable.getLevel()
            && stillValid(ContainerLevelAccess.create(cable.getLevel(), cable.getBlockPos()), player,
                Machines.COMPARING_CABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
