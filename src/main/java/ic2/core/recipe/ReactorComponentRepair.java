package ic2.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record ReactorComponentRepair(Ingredient component, Ingredient material, int materialCount, int repair) implements CraftingRecipe {
    private static final MapCodec<ReactorComponentRepair> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("component").forGetter(ReactorComponentRepair::component),
        Ingredient.CODEC_NONEMPTY.fieldOf("material").forGetter(ReactorComponentRepair::material),
        Codec.intRange(1, 8).optionalFieldOf("material_count", 1).forGetter(ReactorComponentRepair::materialCount),
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("repair").forGetter(ReactorComponentRepair::repair)
    ).apply(instance, ReactorComponentRepair::new));

    public static final RecipeSerializer<ReactorComponentRepair> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ReactorComponentRepair> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReactorComponentRepair> streamCodec() {
            return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
        }
    };

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !damagedComponent(input).isEmpty();
    }

    private ItemStack damagedComponent(CraftingInput input) {
        if (input.ingredientCount() != materialCount + 1) {
            return ItemStack.EMPTY;
        }
        ItemStack damaged = ItemStack.EMPTY;
        int materials = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isDamaged() && component.test(stack)) {
                damaged = stack;
            } else if (material.test(stack)) {
                materials++;
            }
        }
        return materials == materialCount ? damaged : ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return repairComponent(damagedComponent(input));
    }

    public ItemStack repairComponent(ItemStack component) {
        ItemStack result = component.copyWithCount(1);
        if (!result.isEmpty()) {
            result.setDamageValue(Math.max(0, result.getDamageValue() - repair));
        }
        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= materialCount + 1;
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
        return IC2.REACTOR_COMPONENT_REPAIR.get();
    }
}
