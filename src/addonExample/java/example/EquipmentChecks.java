package example;

import ic2.api.Ic2Api;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class EquipmentChecks {
    static void run(ServerLevel level) {
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
