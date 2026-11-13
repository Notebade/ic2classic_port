package ic2.core.recipe;

import com.mojang.serialization.MapCodec;
import ic2.api.item.Toolbox;
import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Native toolbox upgrade keeps the original inventory, including legacy non-tool contents. */
public final class ToolboxUpgradeRecipe extends ShapedRecipe {
    private final ShapedRecipe base;
    private static final MapCodec<ToolboxUpgradeRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(ToolboxUpgradeRecipe::new, recipe -> recipe.base);
    public static final RecipeSerializer<ToolboxUpgradeRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<ToolboxUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ToolboxUpgradeRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    };
    private ToolboxUpgradeRecipe(ShapedRecipe base) {
        super(base.getGroup(), base.category(), base.pattern, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        this.base = base;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level) || !(getResultItem(level.registryAccess()).getItem() instanceof Toolbox target)) return false;
        var sources = input.items().stream().filter(stack -> stack.getItem() instanceof Toolbox).toList();
        return sources.size() == 1 && ((Toolbox) sources.getFirst().getItem()).slots() <= target.slots();
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        var result = super.assemble(input, lookup);
        for (var stack : input.items()) if (stack.getItem() instanceof Toolbox) {
            result.set(DataComponents.CONTAINER, stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            break;
        }
        return result;
    }
    @Override public RecipeSerializer<?> getSerializer() { return IC2.TOOLBOX_UPGRADE_RECIPE.get(); }
}
