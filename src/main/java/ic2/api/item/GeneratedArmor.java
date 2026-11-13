package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** Generation-only armor buffer, distinct from externally chargeable ElectricItem storage. */
public interface GeneratedArmor {
    int generatedEnergy(ItemStack stack);
    int generationCapacity();
    void setGeneratedEnergy(ItemStack stack, int energy);
}
