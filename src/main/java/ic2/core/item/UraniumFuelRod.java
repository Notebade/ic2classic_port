package ic2.core.item;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class UraniumFuelRod extends Item {
    private final int cells;
    private final UraniumType type;
    private final Supplier<? extends Item> depleted;

    public UraniumFuelRod(Properties properties, int cells) {
        this(properties, cells, UraniumType.STANDARD, IC2.DEPLETED_URANIUM_ROD);
    }

    public UraniumFuelRod(Properties properties, int cells, UraniumType type, Supplier<? extends Item> depleted) {
        super(properties.durability(type.durability()).setNoRepair());
        this.cells = cells;
        this.type = type;
        this.depleted = depleted;
    }

    public static int cellsIn(ItemStack stack) {
        return stack.getItem() instanceof UraniumFuelRod rod ? rod.cells : 0;
    }

    public int cells() {
        return cells;
    }

    public UraniumType type() {
        return type;
    }

    public int selfPulsesPerCell() {
        return (1 + cells / 2) * type.pulses();
    }

    public ItemStack depletedRods() {
        return new ItemStack(depleted.get(), cells);
    }
}
