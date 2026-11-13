package ic2.core.recipe;

import com.mojang.serialization.MapCodec;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Original IC2 shaped crafting carries ingredient EU into a single electric result. */
public final class ElectricShapedRecipe extends ShapedRecipe {
    private final ShapedRecipe base;
    private static final MapCodec<ElectricShapedRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(ElectricShapedRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<ElectricShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<ElectricShapedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ElectricShapedRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    };
    private ElectricShapedRecipe(ShapedRecipe base) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        this.base = base;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) { return ElectricCrafting.carry(input, super.assemble(input, lookup)); }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.ELECTRIC_SHAPED_RECIPE.get(); }
}
