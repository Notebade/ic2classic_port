package ic2.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import ic2.core.recipe.CountedCrafting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
abstract class ResultSlotMixin {
    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ic2$checkFuelCellRecipe(Player player, ItemStack result, CallbackInfo callback,
                                        @Share("countedRecipe") LocalRef<CountedCrafting> countedRecipe) {
        var level = player.level();
        var input = craftSlots.asCraftInput();
        countedRecipe.set(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
            .map(recipe -> recipe.value() instanceof CountedCrafting counted ? counted : null).orElse(null));
    }

    @WrapOperation(method = "onTake", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ic2$consumeFuelCellIngredients(CraftingContainer input, int slot, int count,
            Operation<ItemStack> original, @Share("countedRecipe") LocalRef<CountedCrafting> countedRecipe) {
        int consumed = countedRecipe.get() == null ? count : countedRecipe.get().consumed(input.getItem(slot));
        return original.call(input, slot, consumed);
    }
}
