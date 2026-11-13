package example;

import ic2.api.Ic2Api;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class RecipeUnlockChecks {
    static void run(ServerLevel level) {
        var server = level.getServer();
        for (String name : java.util.List.of("electric_fisher", "ranged_pump", "overclocked_pump", "miner", "electric_enchanter", "colossal_furnace", "su_battery_0"))
            require(server.getAdvancements().get(ResourceLocation.parse("ic2:recipes/energy_advanced/" + name)) != null, "recipe book discovery advancement exists: " + name);
    }
    static void verifyPlayer(net.minecraft.server.level.ServerPlayer player) {
        var level = player.serverLevel(); var server = level.getServer();
        var advancement = server.getAdvancements().get(ResourceLocation.parse("ic2:recipes/energy_advanced/electric_fisher"));
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:electric_fisher")).orElseThrow();
        for (String criterion : advancement.value().criteria().keySet()) player.getAdvancements().revoke(advancement, criterion);
        player.resetRecipes(java.util.List.of(recipe));
        require(!player.getRecipeBook().contains(recipe), "fixture begins with a locked fisher recipe");
        var ingredient = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:complex_circuit")).orElseThrow());
        var previous = player.getInventory().getItem(0);
        try {
            player.getInventory().setItem(0, ingredient);
            net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), ingredient);
        } finally { player.getInventory().setItem(0, previous); }
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        require(player.getRecipeBook().contains(recipe), "actual inventory discovery unlocks the fisher recipe; completed=" + progress.getCompletedCriteria() + "; done=" + progress.isDone());
        var result = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:electric_fisher")).orElseThrow());
        var stat = net.minecraft.stats.Stats.ITEM_CRAFTED.get(result.getItem()); int crafted = player.getStats().getValue(stat);
        try { result.onCraftedBy(level, player, 1); require(player.getStats().getValue(stat) == crafted + 1, "native crafted-item hook records the new machine in player statistics"); }
        finally { player.getStats().setValue(player, stat, crafted); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
