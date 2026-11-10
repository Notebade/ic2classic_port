package ic2.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import ic2.core.IC2;
import ic2.core.recipe.ReactorFuelCellRecipe;
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
                                        @Share("fuelCellRecipe") LocalBooleanRef fuelCellRecipe) {
        var level = player.level();
        var input = craftSlots.asCraftInput();
        fuelCellRecipe.set(input.items().stream().anyMatch(stack -> stack.is(IC2.NETHERRACK_DUST.get()))
            && level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
            .filter(recipe -> recipe.value() instanceof ReactorFuelCellRecipe).isPresent());
    }

    @WrapOperation(method = "onTake", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ic2$consumeFuelCellIngredients(CraftingContainer input, int slot, int count,
            Operation<ItemStack> original, @Share("fuelCellRecipe") LocalBooleanRef fuelCellRecipe) {
        int consumed = fuelCellRecipe.get() ? ReactorFuelCellRecipe.consumption(input.getItem(slot)) : count;
        return original.call(input, slot, consumed);
    }
}
