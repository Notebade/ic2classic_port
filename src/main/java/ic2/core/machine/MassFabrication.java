package ic2.core.machine;

import ic2.core.IC2;
import net.minecraft.world.item.ItemStack;

final class MassFabrication {
    static final int MATTER_COST = 7_000_000;
    static final int MAX_INPUT = 512;
    static final int BONUS = 5;
    static final int REFILL_BATCH = 10;

    private MassFabrication() {}

    static int amplifierValue(ItemStack stack) {
        if (stack.is(IC2.SCRAP.get())) {
            return 1_000;
        }
        if (stack.is(IC2.SCRAP_BOX.get())) {
            return 45_000;
        }
        if (stack.is(IC2.SCRAP_METAL.get())) {
            return 100_000;
        }
        return 0;
    }
}
