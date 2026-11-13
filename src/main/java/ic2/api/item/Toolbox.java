package ic2.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Tool-only portable storage. Reads return copies; rejected writes must leave contents untouched. */
public interface Toolbox {
    int slots();
    boolean accepts(ItemStack item);
    ItemStack stored(ItemStack box, int slot);
    boolean store(ItemStack box, int slot, ItemStack item);
    /** Server-side cycle between the player's held tool and a nonempty box slot; ownership is required. */
    boolean scroll(ItemStack box, Player player, int direction);
}
