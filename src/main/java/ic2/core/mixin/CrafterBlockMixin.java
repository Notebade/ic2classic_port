package ic2.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ic2.core.recipe.CountedCrafting;
import java.util.function.Consumer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrafterBlock.class)
abstract class CrafterBlockMixin {
    @WrapOperation(method = "dispenseFrom", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/NonNullList;forEach(Ljava/util/function/Consumer;)V"))
    private void ic2$consumeFuelCellIngredients(NonNullList<ItemStack> items, Consumer<ItemStack> consumer,
            Operation<Void> original, @Local RecipeHolder<CraftingRecipe> recipe) {
        if (recipe.value() instanceof CountedCrafting counted) {
            for (ItemStack stack : items) {
                stack.shrink(counted.consumed(stack));
            }
        } else {
            original.call(items, consumer);
        }
    }
}
