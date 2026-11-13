package ic2.api.machine;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface DirectionalMachineUpgrade extends MachineUpgrade {
    Optional<Direction> facing(ItemStack stack);
    void setFacing(ItemStack stack, Optional<Direction> direction);
}
