package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.ReactorHeatItem;
import ic2.core.item.ReactorReflectorItem;
import ic2.core.item.UraniumFuelRod;
import ic2.core.recipe.ReactorComponentRepair;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

final class CanningRecipes {
    static final int MAX_SPEED = 10_000;
    static final int SPEED_DIVISOR = 30;
    private static final TagKey<Item> COAL_DUSTS = TagKey.create(Registries.ITEM, id("coal_dusts"));
    private static final List<ResourceLocation> REPAIRS = List.of(
        id("condensator_repair_redstone"), id("condensator_lap_repair_redstone"), id("condensator_lap_repair_lapis"),
        id("reflector_iridium_repair_coal"), id("reflector_iridium_repair_charcoal"));
    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> filling = RecipeManager.createCheck(RecipeType.CRAFTING);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, path);
    }

    private static Item can(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id(path));
    }

    private static int portions(ItemStack food) {
        var properties = food.getFoodProperties(null);
        return properties == null ? food.is(Items.CAKE) ? 6 : 0 : Math.max(1, (properties.nutrition() + 1) / 2);
    }

    static boolean acceptsContainer(ItemStack stack) {
        return stack.is(can("tin_can")) || stack.is(IC2.CELL_EMPTY.get()) || stack.getItem() instanceof ReactorHeatItem.Condensator
            || stack.getItem() instanceof ReactorReflectorItem reflector && reflector.repairable();
    }

    static boolean acceptsMaterial(ItemStack stack) {
        return portions(stack) > 0 || isUranium(stack) || stack.is(BoilerFuel.COMPRESSED_COAL.get()) || stack.is(BoilerFuel.COMPRESSED_PLANT.get())
            || stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI) || stack.is(COAL_DUSTS);
    }

    private static boolean isUranium(ItemStack stack) {
        return stack.is(IC2.URANIUM_INGOT.get()) || IC2.ENRICHED_URANIUM.isIngot(stack);
    }

    Batch find(ItemStack container, ItemStack material, Level level) {
        if (container.isEmpty() || material.isEmpty() || level == null) {
            return null;
        }
        if (container.is(can("tin_can"))) {
            int count = portions(material);
            return count > 0 && container.getCount() >= count
                ? new Batch(new ItemStack(can(ic2.core.equipment.FoodCans.outputId(material)), count), 1, false, count, count * 50) : null;
        }
        if (container.is(IC2.CELL_EMPTY.get())) {
            if (material.is(BoilerFuel.COMPRESSED_COAL.get())) return new Batch(new ItemStack(BoilerFuel.HYDRATED_CELL.get()), 1, false);
            if (material.is(BoilerFuel.COMPRESSED_PLANT.get())) return new Batch(new ItemStack(BoilerFuel.RAW_BIO_CELL.get()), 1, false);
        }
        if (container.is(IC2.CELL_EMPTY.get()) && isUranium(material)) {
            var input = CraftingInput.of(2, 1, List.of(container.copyWithCount(1), material.copyWithCount(1)));
            ItemStack rod = filling.getRecipeFor(input, level)
                .map(recipe -> recipe.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
            return rod.getItem() instanceof UraniumFuelRod ? new Batch(rod, 1, false) : null;
        }
        if (!container.isDamaged()) {
            return null;
        }
        for (ResourceLocation id : REPAIRS) {
            var recipe = level.getRecipeManager().byKey(id);
            if (recipe.isPresent() && recipe.get().value() instanceof ReactorComponentRepair repair
                    && repair.component().test(container) && repair.material().test(material)
                    && material.getCount() >= repair.materialCount()) {
                return new Batch(repair.repairComponent(container), repair.materialCount(), true);
            }
        }
        return null;
    }

    record Batch(ItemStack output, int materialCount, boolean repair, int containers, int ticks) {
        Batch(ItemStack output, int materialCount, boolean repair) {
            this(output, materialCount, repair, 1, 0);
        }
    }
}
