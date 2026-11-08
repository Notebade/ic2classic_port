package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.IC2;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record UuMatterRecipe(List<String> pattern, ItemStack result) implements CraftingRecipe {
    private static final Codec<List<String>> PATTERN_CODEC = Codec.STRING.listOf().validate(pattern -> {
        if (pattern.size() == 3 && pattern.stream().allMatch(row -> row.matches("[ M]{3}"))
                && pattern.stream().anyMatch(row -> row.contains("M"))) {
            return DataResult.success(pattern);
        }
        return DataResult.error(() -> "UU-matter pattern must have three rows of three spaces or M, with at least one M");
    });
    private static final MapCodec<UuMatterRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        PATTERN_CODEC.fieldOf("pattern").forGetter(UuMatterRecipe::pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(UuMatterRecipe::result)
    ).apply(instance, UuMatterRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, UuMatterRecipe> STREAM_CODEC =
        ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public static final RecipeSerializer<UuMatterRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<UuMatterRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UuMatterRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int offset = ((UuCraftingGrid) input).ic2$getGridOffset();
        if (offset < 0) {
            return false;
        }

        int left = offset % 3;
        int top = offset / 3;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int inputX = x - left;
                int inputY = y - top;
                ItemStack stack = ItemStack.EMPTY;
                if (inputX >= 0 && inputX < input.width() && inputY >= 0 && inputY < input.height()) {
                    stack = input.getItem(inputX, inputY);
                }
                if (pattern.get(y).charAt(x) == 'M' ? !stack.is(IC2.UU_MATTER.get()) : !stack.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return IC2.UU_MATTER_RECIPE.get();
    }
}
