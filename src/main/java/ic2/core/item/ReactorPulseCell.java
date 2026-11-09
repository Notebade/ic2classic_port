package ic2.core.item;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ReactorPulseCell extends Item {
    private final Supplier<? extends Item> result;
    private final int heatPerBonusPulse;

    public ReactorPulseCell(Properties properties, int breedingRequirement, Supplier<? extends Item> result) {
        this(properties, breedingRequirement, result, 3_000);
    }

    public ReactorPulseCell(Properties properties, int breedingRequirement, Supplier<? extends Item> result, int heatPerBonusPulse) {
        super(properties.durability(breedingRequirement).component(DataComponents.DAMAGE, breedingRequirement).setNoRepair());
        this.result = result;
        this.heatPerBonusPulse = heatPerBonusPulse;
    }

    public int progressPerPulse(int hullHeat) {
        return 1 + hullHeat / heatPerBonusPulse;
    }

    public ItemStack activated() {
        return new ItemStack(result.get());
    }
}
