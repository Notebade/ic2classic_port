package ic2.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class UraniumFuelRod extends Item {
    private final int cells;

    public UraniumFuelRod(Properties properties, int cells) {
        super(properties.durability(10_000).setNoRepair());
        this.cells = cells;
    }

    public static int cellsIn(ItemStack stack) {
        return stack.getItem() instanceof UraniumFuelRod rod ? rod.cells : 0;
    }
}
