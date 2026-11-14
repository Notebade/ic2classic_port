package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** Foam quantities are blocks, independent of EU. Canners refill in whole source batches. */
public interface FoamContainer {
    int foamCapacity(ItemStack stack);
    int storedFoam(ItemStack stack);
    void setFoam(ItemStack stack, int amount);
    default int refillAmount() { return 26; }
    default boolean canRefill(ItemStack stack) { return foamCapacity(stack) - storedFoam(stack) >= refillAmount(); }
    default boolean suppliesWornFoam() { return false; }
}
