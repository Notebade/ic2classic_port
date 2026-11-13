package example;

import ic2.api.Ic2Api;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Integration fixture deliberately inspecting internal JEI data, outside the addon API contract. */
public final class RecipeAvailabilityChecks {
    private RecipeAvailabilityChecks() {}

    public static void run(ServerLevel level) throws Exception {
        for (var file : level.getServer().getResourceManager().listResources("recipe", id -> id.getNamespace().equals("ic2") && id.getPath().endsWith(".json")).keySet()) {
            var id = ResourceLocation.fromNamespaceAndPath("ic2", file.getPath().substring("recipe/".length(), file.getPath().length() - 5));
            if (level.getRecipeManager().byKey(id).isEmpty()) throw new AssertionError("IC2 recipe resource failed to load: " + id);
        }
        for (var recipe : level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (!recipe.id().getNamespace().equals("ic2")) continue;
            for (var ingredient : recipe.value().getIngredients())
                if (!ingredient.isEmpty() && ingredient.hasNoItems()) throw new AssertionError("Crafting recipe references an empty ingredient/tag: " + recipe.id());
        }
        var processType = Class.forName("ic2.core.machine.MachineRecipeView$Process");
        var process = java.util.Arrays.stream(processType.getEnumConstants())
            .filter(value -> value.toString().equals("URANIUM_ENRICHER")).findFirst().orElseThrow();
        var method = Class.forName("ic2.core.machine.MachineRecipeViews").getDeclaredMethod("forProcess", processType, net.minecraft.world.level.Level.class);
        method.setAccessible(true);
        var obtainable = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (var recipe : level.getRecipeManager().getRecipes())
            obtainable.add(recipe.value().getResultItem(level.registryAccess()).getItem());
        for (Object category : processType.getEnumConstants()) {
            for (Object recipe : (List<?>) method.invoke(null, category, level)) {
                for (Object slot : (List<?>) value(recipe, "outputs")) {
                    for (Object stack : (List<?>) value(slot, "items")) obtainable.add(((ItemStack) stack).getItem());
                }
            }
        }
        for (var id : Ic2Api.itemIds()) {
            if (id.getPath().startsWith("ingot_") && !obtainable.contains(Ic2Api.item(id).orElseThrow()))
                throw new AssertionError("No crafting, smelting or JEI machine acquisition route for " + id);
        }
        var recipes = (List<?>) method.invoke(null, process, level);
        for (String family : List.of("blaze", "charcoal", "enderpearl", "netherstar", "redstone")) {
            var output = Ic2Api.item(ResourceLocation.parse("ic2:ingot_uranium_enriched_" + family)).orElseThrow();
            int count = 0;
            for (Object recipe : recipes) {
                var outputs = (List<?>) value(recipe, "outputs");
                var result = (ItemStack) ((List<?>) value(outputs.getFirst(), "items")).getFirst();
                if (!result.is(output)) continue;
                count++;
                var inputs = (List<?>) value(recipe, "inputs");
                var catalyst = ((ItemStack) ((List<?>) value(inputs.get(1), "items")).getFirst()).copy();
                var pos = new BlockPos(112, 80, 112);
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:uranium_enricher")).orElseThrow().defaultBlockState());
                var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (inventory == null) throw new AssertionError("enricher inventory unavailable");
                inventory.insertItem(0, new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_uranium")).orElseThrow()), false);
                inventory.insertItem(1, catalyst, false);
                for (int tick = 0; tick < 2000; tick++) {
                    ((ic2.api.energy.EnergySink) level.getBlockEntity(pos)).receiveEnergy(512);
                    ApiSmokeChecks.tick(level, pos, level.getBlockEntity(pos).getType());
                }
                var results = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, net.minecraft.core.Direction.DOWN);
                boolean produced = results != null && results.getStackInSlot(0).is(output);
                if (!produced) throw new AssertionError("displayed recipe does not produce " + family);
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            if (count == 0) throw new AssertionError("JEI acquisition route missing: " + family);
        }
        com.mojang.logging.LogUtils.getLogger().info("IC2 ingot recipe availability checks passed (all ingots, five enrichment families)");
    }

    private static Object value(Object record, String field) throws Exception {
        var method = record.getClass().getDeclaredMethod(field);
        method.setAccessible(true);
        return method.invoke(record);
    }
}
