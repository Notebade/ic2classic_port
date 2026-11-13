package ic2.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Native transfer stops after one empty slot. Complete an oversized crafting batch using individual legal stacks. */
@Mixin(AbstractContainerMenu.class)
abstract class BatchCraftingMixin {
    @WrapMethod(method = "moveItemStackTo")
    private boolean ic2$splitBatch(ItemStack stack, int start, int end, boolean reverse, Operation<Boolean> original) {
        if (stack.getCount() <= stack.getMaxStackSize()) return original.call(stack, start, end, reverse);
        boolean moved = false;
        while (!stack.isEmpty()) {
            int before = stack.getCount();
            moved |= original.call(stack, start, end, reverse);
            if (stack.getCount() >= before) break;
        }
        return moved;
    }
}
