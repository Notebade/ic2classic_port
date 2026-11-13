package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class EquipmentChecks {
    static void run(ServerLevel level) {
        checkDrills(level);
        checkAreaDrill(level);
        checkFortuneDrill(level);
        checkNanoShield(level);
        for (String material : java.util.List.of("bronze", "aluminium", "composite")) {
            var shield = new ItemStack(Ic2Api.item(id(material + "_shield")).orElseThrow(() -> new AssertionError("source shield exists: " + material)));
            require(shield.getMaxDamage() == switch(material) { case "bronze" -> 1000; case "aluminium" -> 2000; default -> 3000; }, "source shield durability");
            require(shield.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHIELD_BLOCK), "shield actually blocks");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shield);
            shield.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            require(player.isUsingItem() && player.getUseItem() == shield, "shield use starts blocking");
            player.stopUsingItem();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        for (String material : java.util.List.of("bronze", "composite")) {
            for (String piece : java.util.List.of("helmet", "chest", "leggings", "boots")) {
                var item = Ic2Api.item(id("armor_" + material + "_" + piece)).orElseThrow(() -> new AssertionError("source armor exists: " + material + " " + piece));
                require(item instanceof net.minecraft.world.item.ArmorItem, "armor can actually equip");
                var armor = (net.minecraft.world.item.ArmorItem) item;
                require(armor.getDefense() == (material.equals("bronze") ? switch(piece) { case "helmet", "boots" -> 3; case "leggings" -> 6; default -> 8; } : switch(piece) { case "helmet", "boots" -> 4; case "leggings" -> 7; default -> 9; }), "source armor defense");
                require(new ItemStack(item).getMaxDamage() == armor.getType().getDurability(material.equals("bronze") ? 15 : 50), "source armor durability");
            }
        }
        for (String material : java.util.List.of("bronze", "aluminium")) {
            for (String tool : java.util.List.of("axe", "hoe", "pickaxe", "shovel", "sword"))
                require(Ic2Api.item(id("tool_" + tool + "_" + material)).isPresent(), "source conventional equipment exists: " + material + " " + tool);
            var stack = new ItemStack(Ic2Api.item(id("tool_pickaxe_" + material)).orElseThrow());
            require(stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()) && !stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "source conventional pickaxe retains iron harvest tier");
            require(stack.getMaxDamage() == (material.equals("bronze") ? 350 : 700)
                && stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) == (material.equals("bronze") ? 6 : 10), "source durability and mining speed");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            stack.getItem().mineBlock(stack, level, Blocks.STONE.defaultBlockState(), new BlockPos(0, 100, 0), player);
            require(stack.getDamageValue() == 1, "actual conventional mining consumes one durability");
            require(stack.getItem().isValidRepairItem(stack, new ItemStack(Ic2Api.item(id("ingot_" + material)).orElseThrow())), "matching ingot repairs the source tool");
            checkRecipe(level, "tool_pickaxe_" + material, "MMM", " S ", " S ", java.util.Map.of('M', "ic2:ingot_" + material, 'S', "minecraft:stick"));
        }
    }
    private static void checkFortuneDrill(ServerLevel level) {
        var recipe = level.getRecipeManager().byKey(id("diamond_drill_fortune")).orElseThrow(() -> new AssertionError("source counted fortune drill recipe exists"));
        var ids = java.util.List.of("ic2:plate_iridium", "ic2:advanced_circuit", "ic2:plate_iridium", "minecraft:end_crystal", "ic2:diamond_drill", "minecraft:end_crystal", "ic2:upgrade_base", "ic2:magnet", "ic2:upgrade_base");
        var stacks = ids.stream().map(value -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(value)))).toList();
        var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value();
        require(!crafting.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks), level), "fortune drill rejects undersized ingredients");
        stacks.get(1).setCount(5); stacks.get(3).setCount(3); stacks.get(5).setCount(3); stacks.get(7).setCount(5);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks);
        require(crafting.matches(input, level), "fortune drill accepts original stack counts");
        var result = crafting.assemble(input, level.registryAccess());
        require(result.is(Ic2Api.item(id("diamond_drill")).orElseThrow()) && result.getEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)) == 3, "fortune recipe produces fortune III drill");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var menu = new net.minecraft.world.inventory.CraftingMenu(77, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, BlockPos.ZERO));
        for (int slot = 0; slot < 9; slot++) menu.getSlot(slot + 1).set(stacks.get(slot).copy());
        menu.slotsChanged(menu.getSlot(1).container);
        require(!menu.getSlot(0).getItem().isEmpty(), "real crafting menu offers counted recipe");
        var taken = menu.getSlot(0).remove(1);
        menu.getSlot(0).onTake(player, taken);
        for (int slot = 1; slot <= 9; slot++) require(menu.getSlot(slot).getItem().isEmpty(), "real craft consumes full original stack counts");
    }
    private static void checkAreaDrill(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getMainHandItem().copy(); var oldMode = player.gameMode.getGameModeForPlayer();
        var stack = new ItemStack(Ic2Api.item(id("advanced_drill")).orElseThrow());
        var electric = (ic2.api.item.ElectricItem) stack.getItem();
        ((ic2.api.item.ConfigurableTool) stack.getItem()).setMode(stack, "area");
        var center = new BlockPos(450, 100, 450);
        var protectedPos = center.offset(1, 0, 1);
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> protect = event -> { if (event.getPos().equals(protectedPos)) event.setCanceled(true); };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protect);
        try {
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            player.setPos(450.5, 102, 450.5); player.setXRot(90);
            for (var pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2 | 16);
            electric.setCharge(stack, 1000);
            require(player.gameMode.destroyBlock(center), "area drill destroys selected block");
            for (var pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) require(pos.equals(protectedPos) ? level.getBlockState(pos).is(Blocks.STONE) : level.isEmptyBlock(pos), "area drill respects protection per block");
            require(electric.charge(stack) == 360, "area drill only spends energy on eight allowed blocks");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protect);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, saved);
            player.gameMode.changeGameModeForPlayer(oldMode);
        }
    }
    private static void checkDrills(ServerLevel level) {
        checkRecipe(level, "diamond_drill", " D ", "DVD", "   ", java.util.Map.of('D', "minecraft:diamond", 'V', "ic2:drill"));
        checkRecipe(level, "rock_cutter_drill", "XYX", "CVC", "   ", java.util.Map.of('X', "ic2:plate_advanced_alloy", 'Y', "ic2:diamond_drill", 'C', "ic2:upgrade_base", 'V', "minecraft:piston"));
        checkRecipe(level, "advanced_drill", "XYX", "CVC", "BNB", java.util.Map.of('X', "ic2:complex_circuit", 'Y', "ic2:advanced_field_expansion_pad_upgrade", 'C', "ic2:plate_iridium", 'V', "ic2:diamond_drill", 'B', "minecraft:netherite_ingot", 'N', "ic2:plasma_core"));
        var names = java.util.List.of("diamond_drill", "advanced_drill", "rock_cutter_drill");
        int[] costs = {80, 80, 20}, boosts = {4, 11, 101}, machineCosts = {15, 26, 21};
        float[] speeds = {16, 30, 60};
        for (int i = 0; i < names.size(); i++) {
            var stack = new ItemStack(Ic2Api.item(id(names.get(i))).orElseThrow(() -> new AssertionError("source drill variant exists")));
            var drill = (ic2.api.item.MiningDrill) stack.getItem();
            var electric = (ic2.api.item.ElectricItem) stack.getItem();
            require(!drill.canUse(stack) && stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1, "empty drill cannot mine at powered speed");
            electric.setCharge(stack, 1000);
            require(drill.canMine(stack, Blocks.OBSIDIAN.defaultBlockState(), level, BlockPos.ZERO), "upgraded drill harvests obsidian");
            require(stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) == speeds[i], "source drill speed");
            require(drill.progressPerTick(stack, Blocks.STONE.defaultBlockState()) == boosts[i] && drill.machineEuPerTick(stack) == machineCosts[i], "source miner boost and EU cost");
            drill.consumeUse(stack);
            require(electric.charge(stack) == 1000 - costs[i], "source per-block EU drain");
            require(electric.minimumPacket() == (i == 1 ? 512 : 32), "source drill charging tier");
            if (i != 1) {
                var modes = (ic2.api.item.ConfigurableTool) stack.getItem();
                require(modes.setMode(stack, "dirt") && !modes.setMode(stack, "invalid") && modes.mode(stack).equals("dirt"), "mode validation and persistence");
                if (i == 0) require(stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 2, "diamond dirt mode slows by eight");
            }
        }
    }
    private static void checkRecipe(ServerLevel level, String output, String first, String second, String third, java.util.Map<Character, String> ingredients) {
        var stacks = (first + second + third).chars().mapToObj(value -> value == ' ' ? ItemStack.EMPTY : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(ingredients.get((char) value))))).toList();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks);
        var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow(() -> new AssertionError("equipment recipe available: " + output));
        require(recipe.value().assemble(input, level.registryAccess()).is(Ic2Api.item(id(output)).orElseThrow()), "equipment recipe produces expected item: " + output);
    }
    private static void checkNanoShield(ServerLevel level) {
        var shield = new ItemStack(Ic2Api.item(id("nano_shield")).orElseThrow(() -> new AssertionError("source nano shield exists")));
        var electric = (ic2.api.item.ElectricItem) shield.getItem();
        require(electric.capacity() == 100000 && electric.transferRate() == 300 && electric.minimumPacket() == 128 && !electric.canProvideEnergy(), "nano shield source electrical contract");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getMainHandItem().copy();
        try {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shield);
            require(shield.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND).getResult() == net.minecraft.world.InteractionResult.FAIL, "empty nano shield cannot block");
            electric.setCharge(shield, 500);
            shield.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            require(player.isUsingItem(), "charged nano shield blocks");
            shield.hurtAndBreak(3, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            require(electric.charge(shield) == 350 && shield.getDamageValue() == 0 && shield.getCount() == 1, "nano shield replaces durability damage with 50 EU per point");
        } finally {
            player.stopUsingItem();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, saved);
        }
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("ic2", name); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
