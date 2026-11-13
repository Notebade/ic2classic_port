package ic2.api.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Feet-slot hook before native fall damage. Return paid whole damage points, in [0, damage]; simulation is pure. */
public interface FallProtectiveArmor {
    int absorbFall(ItemStack stack, Player wearer, int damage, boolean simulate);
}
