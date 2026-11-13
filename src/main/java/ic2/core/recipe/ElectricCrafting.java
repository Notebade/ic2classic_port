package ic2.core.recipe;

import ic2.api.item.ElectricItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

final class ElectricCrafting {
    private ElectricCrafting() {}
    static ItemStack carry(CraftingInput input, ItemStack output) {
        if (!ElectricItem.supports(output, Integer.MAX_VALUE, true)) return output;
        var target = (ElectricItem) output.getItem();
        long total = target.charge(output);
        for (var stack : input.items()) if (ElectricItem.supports(stack, Integer.MAX_VALUE, true)) total += ((ElectricItem) stack.getItem()).charge(stack);
        target.setCharge(output, (int) Math.min(target.capacity(output), total));
        return output;
    }
}
