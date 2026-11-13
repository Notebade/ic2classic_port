package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.IC2;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Native shaped recipe with per-item stack counts for original IC2 crafting. */
public final class SizedShapedRecipe extends ShapedRecipe implements CountedCrafting {
    private final ShapedRecipe base;
    private final Map<ResourceLocation, Integer> counts;
    private static final MapCodec<SizedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShapedRecipe.Serializer.CODEC.forGetter((SizedShapedRecipe recipe) -> recipe.base),
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.intRange(1, 64)).fieldOf("counts").forGetter(recipe -> recipe.counts)
    ).apply(instance, SizedShapedRecipe::new));
    public static final RecipeSerializer<SizedShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<SizedShapedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SizedShapedRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    };
    private SizedShapedRecipe(ShapedRecipe base, Map<ResourceLocation, Integer> counts) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        this.base = base; this.counts = Map.copyOf(counts);
    }
    @Override public int consumed(ItemStack stack) { return counts.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), 1); }
    @Override public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && input.items().stream().allMatch(stack -> stack.isEmpty() || stack.getCount() >= consumed(stack));
    }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.SIZED_SHAPED_RECIPE.get(); }
}
