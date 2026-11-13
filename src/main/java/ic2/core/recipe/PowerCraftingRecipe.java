package ic2.core.recipe;

import ic2.core.IC2;
import ic2.core.machine.BoilerFuel;
import ic2.core.machine.PowerMachines;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public final class PowerCraftingRecipe extends CustomRecipe implements CountedCrafting {
    public enum Kind { CORE, BOILER, FUEL, BUFFER }
    public static RecipeSerializer<PowerCraftingRecipe> serializer(Kind kind) {
        return new SimpleCraftingRecipeSerializer<>(category -> new PowerCraftingRecipe(category, kind));
    }
    private final Kind kind;
    private PowerCraftingRecipe(CraftingBookCategory category, Kind kind) { super(category); this.kind = kind; }
    private static ItemStack item(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ic2:" + id))); }
    @Override public int consumed(ItemStack stack) {
        if (kind == Kind.CORE) return stack.is(IC2.MAGNET.get()) ? 2 : stack.is(IC2.ADVANCED_ALLOY_PLATE.get()) ? 4 : 1;
        if (kind == Kind.BOILER) return stack.is(item("iron_furnace").getItem()) ? 4 : stack.is(item("circuit").getItem()) ? 2 : 1;
        return 1;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        if (kind == Kind.FUEL) return fuelMatches(input);
        if (input.width() != 3 || input.height() != 3) return false;
        String[] pattern = pattern();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            if (kind == Kind.BUFFER && stack.is(ic2.core.machine.StorageInfrastructure.TOOLBOX.get())
                && stack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyStream().findAny().isPresent()) return false;
            if (pattern[slot].isEmpty()) { if (!stack.isEmpty()) return false; }
            else if (!stack.is(item(pattern[slot]).getItem()) || stack.getCount() < consumed(stack)) return false;
        }
        return true;
    }
    private String[] pattern() {
        if (kind == Kind.BUFFER) return new String[]{"", "upgrade_base", "", "circuit", "toolbox", "circuit", "", "machine_block", ""};
        return kind == Kind.CORE ? new String[]{"magnet", "plate_advanced_alloy", "magnet", "plate_advanced_alloy", "cell_plasma", "plate_advanced_alloy", "magnet", "plate_advanced_alloy", "magnet"}
            : new String[]{"circuit", "fluid_export_upgrade_simple", "circuit", "plating_heat_capacity", "machine_tank", "plating_heat_capacity", "", "iron_furnace", ""};
    }
    public List<List<ItemStack>> displayInputs() {
        if (kind == Kind.FUEL) return List.of(List.of(new ItemStack(BoilerFuel.CAN.get())),
            List.of(new ItemStack(BoilerFuel.COAL_CELL.get()), new ItemStack(BoilerFuel.BIO_CELL.get())));
        return Arrays.stream(pattern()).map(id -> {
            if (id.isEmpty()) return List.<ItemStack>of();
            ItemStack stack = item(id); return List.of(stack.copyWithCount(consumed(stack)));
        }).toList();
    }
    public int displayWidth() { return kind == Kind.FUEL ? 2 : 3; }
    public int displayHeight() { return kind == Kind.FUEL ? 1 : 3; }
    private boolean fuelMatches(CraftingInput input) {
        if (input.ingredientCount() != 2) return false;
        boolean can = false, fuel = false;
        for (ItemStack stack : input.items()) {
            can |= stack.is(BoilerFuel.CAN.get()) && BoilerFuel.amount(stack) <= Integer.MAX_VALUE - 2548;
            fuel |= BoilerFuel.value(stack) > 0;
        }
        return can && fuel;
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        if (kind != Kind.FUEL) return getResultItem(lookup);
        ItemStack can = ItemStack.EMPTY; int fuel = 0;
        for (ItemStack stack : input.items()) {
            if (stack.is(BoilerFuel.CAN.get())) can = stack.copyWithCount(1);
            fuel += BoilerFuel.value(stack);
        }
        if (!can.isEmpty()) BoilerFuel.set(can, BoilerFuel.amount(can) + fuel);
        return can;
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider lookup) {
        if (kind == Kind.BUFFER) return item("machine_buffer");
        return kind == Kind.CORE ? new ItemStack(PowerMachines.PLASMA_CORE.get()) : kind == Kind.BOILER ? item("fuel_boiler_structure").copyWithCount(8) : new ItemStack(BoilerFuel.CAN.get());
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return kind == Kind.FUEL ? width * height >= 2 : width >= 3 && height >= 3; }
    @Override public RecipeSerializer<?> getSerializer() {
        return switch (kind) { case CORE -> IC2.PLASMA_CORE_RECIPE.get(); case BOILER -> IC2.BOILER_CASING_RECIPE.get(); case FUEL -> IC2.FUEL_CAN_RECIPE.get(); case BUFFER -> IC2.MACHINE_BUFFER_RECIPE.get(); };
    }
}
