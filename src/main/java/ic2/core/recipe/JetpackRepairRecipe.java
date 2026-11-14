package ic2.core.recipe;

import com.mojang.serialization.MapCodec;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Source magnet repair only clears SpecialMode, preserving the complete input jetpack. */
public final class JetpackRepairRecipe extends ShapelessRecipe {
    private final ShapelessRecipe base;
    private static final MapCodec<JetpackRepairRecipe> CODEC = new ShapelessRecipe.Serializer().codec().xmap(JetpackRepairRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<JetpackRepairRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<JetpackRepairRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, JetpackRepairRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    };
    private JetpackRepairRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients()); this.base = base;
    }
    private ItemStack pack(CraftingInput input) {
        var target = getResultItem(RegistryAccess.EMPTY).getItem();
        return input.items().stream().filter(stack -> stack.is(target)).findFirst().orElse(ItemStack.EMPTY);
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && pack(input).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("SpecialMode");
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        var result = pack(input).copyWithCount(1);
        if (!result.isEmpty()) CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putBoolean("SpecialMode", false));
        return result;
    }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.JETPACK_REPAIR_RECIPE.get(); }
}
