package ic2.api.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server tick for a worn single stack, ordered feet, legs, chest, head. */
public interface ArmorTickItem {
    void tickArmor(ItemStack armor, Player wearer, EquipmentSlot slot);
}
