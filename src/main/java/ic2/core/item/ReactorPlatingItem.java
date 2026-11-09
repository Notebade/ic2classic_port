package ic2.core.item;

import net.minecraft.world.item.Item;

public final class ReactorPlatingItem extends Item {
    private final int additionalHeat;
    private final float containment;

    public ReactorPlatingItem(Properties properties, int additionalHeat, float containment) {
        super(properties);
        this.additionalHeat = additionalHeat;
        this.containment = containment;
    }

    public int additionalHeat() {
        return additionalHeat;
    }

    public float containment() {
        return containment;
    }
}
