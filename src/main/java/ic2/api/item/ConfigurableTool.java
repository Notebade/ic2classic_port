package ic2.api.item;

import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Server-owned tool modes. Unknown modes must be rejected without changing the stack. */
public interface ConfigurableTool {
    List<String> modes();
    String mode(ItemStack stack);
    boolean setMode(ItemStack stack, String mode);
}
