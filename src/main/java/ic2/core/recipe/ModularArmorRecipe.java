package ic2.core.recipe;

import com.mojang.serialization.MapCodec;
import ic2.api.item.ModularArmor;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Default suit crafting installs its source battery; enhancement copies the entire input stack. */
public final class ModularArmorRecipe extends ShapedRecipe {
    private final ShapedRecipe base;
    private final boolean enhance;
    public static RecipeSerializer<ModularArmorRecipe> serializer(boolean enhance) {
        MapCodec<ModularArmorRecipe> codec = ShapedRecipe.Serializer.CODEC.xmap(b -> new ModularArmorRecipe(b, enhance), r -> r.base);
        return new RecipeSerializer<>() {
            @Override public MapCodec<ModularArmorRecipe> codec() { return codec; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, ModularArmorRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(codec.codec()); }
        };
    }
    private ModularArmorRecipe(ShapedRecipe base, boolean enhance) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        this.base = base; this.enhance = enhance;
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) {
        var baseResult = base.getResultItem(lookup);
        if (!(baseResult.getItem() instanceof ModularArmor item)) return baseResult;
        var result = item.defaultArmor(); if (enhance) item.setEnhanced(result, true); return result;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) return false;
        var result = base.getResultItem(level.registryAccess());
        if (!(result.getItem() instanceof ModularArmor)) return false;
        return !enhance || input.items().stream().filter(s -> s.is(result.getItem()) && s.getCount() == 1).count() == 1;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        var result = getResultItem(lookup);
        if (!enhance) return ElectricCrafting.carry(input, result);
        for (var stack : input.items()) if (stack.is(result.getItem())) {
            var copy = stack.copyWithCount(1); return ((ModularArmor) copy.getItem()).setEnhanced(copy, true) ? copy : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() { return enhance ? IC2.MODULAR_ARMOR_UPGRADE_RECIPE.get() : IC2.MODULAR_ARMOR_RECIPE.get(); }
}
