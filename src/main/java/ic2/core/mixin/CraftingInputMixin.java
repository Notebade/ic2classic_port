package ic2.core.mixin;

import ic2.core.recipe.UuCraftingGrid;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingInput.class)
abstract class CraftingInputMixin implements UuCraftingGrid {
    @Unique
    private int ic2$gridOffset = -1;

    @Inject(method = "ofPositioned", at = @At("RETURN"))
    private static void ic2$rememberGridOffset(int width, int height, List<ItemStack> items,
            CallbackInfoReturnable<CraftingInput.Positioned> callback) {
        CraftingInput.Positioned positioned = callback.getReturnValue();
        if (width == 3 && height == 3 && !positioned.input().isEmpty()) {
            // Vanilla trims empty outer rows and columns from recipe inputs.
            ((UuCraftingGrid) positioned.input()).ic2$setGridOffset(positioned.top() * 3 + positioned.left());
        }
    }

    @Override
    public int ic2$getGridOffset() {
        return ic2$gridOffset;
    }

    @Override
    public void ic2$setGridOffset(int offset) {
        ic2$gridOffset = offset;
    }
}
