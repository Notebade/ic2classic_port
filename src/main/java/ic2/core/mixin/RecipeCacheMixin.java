package ic2.core.mixin;

import ic2.core.IC2;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeCache.class)
abstract class RecipeCacheMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void ic2$findUuRecipe(Level level, CraftingInput input,
            CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> callback) {
        // The crafter cache keys use trimmed slots and omit their original offset.
        for (ItemStack stack : input.items()) {
            if (stack.is(IC2.UU_MATTER.get())) {
                callback.setReturnValue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level));
                return;
            }
        }
    }
}
