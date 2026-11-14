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

/** Source ModularArmorModifier changes the shell item while retaining every input component. */
public final class ModularArmorConversionRecipe extends ShapedRecipe {
    private static final MapCodec<ModularArmorConversionRecipe> CODEC = ShapedRecipe.Serializer.CODEC
        .xmap(ModularArmorConversionRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<ModularArmorConversionRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<ModularArmorConversionRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ModularArmorConversionRecipe> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };
    private final ShapedRecipe base;

    private ModularArmorConversionRecipe(ShapedRecipe base) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        this.base = base;
    }
    private ItemStack source(CraftingInput input) {
        ItemStack source = ItemStack.EMPTY;
        for (var stack : input.items()) if (stack.getItem() instanceof ModularArmor) {
            if (!source.isEmpty() || stack.getCount() != 1) return ItemStack.EMPTY;
            source = stack;
        }
        return source;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && !source(input).isEmpty()
            && getResultItem(level.registryAccess()).getItem() instanceof ModularArmor;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        var source = source(input);
        return source.isEmpty() ? ItemStack.EMPTY : source.transmuteCopy(getResultItem(lookup).getItem(), 1);
    }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.MODULAR_ARMOR_CONVERSION_RECIPE.get(); }
}
