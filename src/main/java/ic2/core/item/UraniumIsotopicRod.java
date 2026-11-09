package ic2.core.item;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class UraniumIsotopicRod extends Item {
    private final Supplier<? extends Item> reEnriched;

    public UraniumIsotopicRod(Properties properties, int breedingRequirement, Supplier<? extends Item> reEnriched) {
        super(properties.durability(breedingRequirement).component(DataComponents.DAMAGE, breedingRequirement).setNoRepair());
        this.reEnriched = reEnriched;
    }

    public ItemStack reEnrichedRod() {
        return new ItemStack(reEnriched.get());
    }
}
