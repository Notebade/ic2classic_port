package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** A crafting batch can return several individually nonstackable items; native inventory insertion splits them. */
public final class BatchShapelessRecipe extends ShapelessRecipe {
    private final ShapelessRecipe base;
    private final int count;
    private static final MapCodec<BatchShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        new ShapelessRecipe.Serializer().codec().forGetter((BatchShapelessRecipe r) -> r.base),
        Codec.intRange(1, 64).fieldOf("output_count").forGetter(r -> r.count)
    ).apply(i, BatchShapelessRecipe::new));
    public static final RecipeSerializer<BatchShapelessRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<BatchShapelessRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BatchShapelessRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    };
    private BatchShapelessRecipe(ShapelessRecipe base, int count) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients()); this.base = base; this.count = count;
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) { return base.getResultItem(lookup).copyWithCount(count); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) { return getResultItem(lookup); }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.BATCH_SHAPELESS_RECIPE.get(); }
}
