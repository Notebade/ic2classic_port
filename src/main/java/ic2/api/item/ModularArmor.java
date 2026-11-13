package ic2.api.item;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Native module custody: successful install consumes one item; removal returns it, simulation never mutates. */
public interface ModularArmor extends ElectricItem {
    EquipmentSlot equipmentSlot();
    Map<ResourceLocation, Integer> moduleLimits(ItemStack armor);
    List<InstalledArmorModule> modules(ItemStack armor);
    Set<ResourceLocation> features(ItemStack armor);
    boolean canInstall(ItemStack armor, ResourceLocation category, ItemStack module);
    boolean install(ItemStack armor, ResourceLocation category, ItemStack module);
    ItemStack removeModule(ItemStack armor, int index, boolean simulate);
    ItemStack defaultArmor();
    boolean enhanced(ItemStack armor);
    boolean setEnhanced(ItemStack armor, boolean enhanced);
}
