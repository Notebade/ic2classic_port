package ic2.core.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compatibility entry point for the first Remastered processor. */
final class AlloySmelterScreen extends ProcessingMachineScreen<MachineMenu> {
    AlloySmelterScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
