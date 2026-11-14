package example;

import ic2.api.Ic2Api;
import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class ArmorSuiteChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("ic2.armor.verify")) return;
        try {
            ModularArmorChecks.run(event.getServer().overworld());
            com.mojang.logging.LogUtils.getLogger().info("IC2 complete armor server checks passed");
        } finally { event.getServer().halt(false); }
    }

    static void run(ServerLevel level) {
        var failures = new java.util.ArrayList<String>();
        for (Runnable check : java.util.List.<Runnable>of(() -> dyeAndWash(level), () -> categoryAdmission(), () -> configuratorLayout(level), () -> conversion(level))) {
            try { check.run(); } catch (AssertionError failure) { failures.add(failure.getMessage()); }
        }
        require(failures.isEmpty(), String.join("; ", failures));
    }

    private static void conversion(ServerLevel level) {
        for (String piece : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
            var nano = (ModularArmor) stack("nano_suit_" + piece).getItem();
            var inputArmor = nano.defaultArmor();
            nano.setEnhanced(inputArmor, true);
            nano.setCharge(inputArmor, 4321);
            require(nano.install(inputArmor, ArmorModule.GENERIC, stack("lava_module")), "upgrade fixture has extra module");
            inputArmor.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Preserve me"));
            if (piece.equals("chestplate")) {
                require(nano.install(inputArmor, ArmorModule.STORAGE, stack("tank_storage_module")), "upgrade fixture has tank");
                inputArmor.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM).fill(
                    new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1234),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
            var before = inputArmor.copy();
            var keys = java.util.Map.of('n', inputArmor, 'I', stack("plate_iridium"), 'L', stack("base_module"),
                'A', stack("plate_advanced_alloy"), 'G', piece.equals("helmet") ? stack("reinforced_glass") : new ItemStack(Items.GLOWSTONE_DUST),
                'C', stack("advanced_circuit"), 'M', stack("machine_block"), 'R', stack("armor_hazmat_boots"));
            var pattern = switch (piece) { case "helmet" -> " n ILICGC"; case "chestplate" -> "AnAILIIAI"; case "leggings" -> "MLMInIG G"; default -> "InIRLR"; };
            var ingredients = pattern.chars().mapToObj(c -> keys.getOrDefault((char) c, ItemStack.EMPTY)).toList();
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, pattern.length() / 3, ingredients);
            var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager()
                .byKey(ResourceLocation.parse("ic2:empty_quantum_armor_" + piece)).orElseThrow().value();
            require(recipe.matches(input, level), "source empty quantum recipe matches " + piece);
            var output = recipe.assemble(input, level.registryAccess());
            var quantum = (ModularArmor) output.getItem();
            require(output.is(stack("quantum_suit_" + piece).getItem()) && quantum.charge(output) == 4321
                && quantum.capacity(output) == 100000 && quantum.enhanced(output)
                && quantum.modules(output).size() == nano.modules(inputArmor).size()
                && quantum.features(output).contains(ArmorModule.id("lava_protection"))
                && output.getHoverName().getString().equals("Preserve me"), "empty quantum recipe preserves nano modules, EU and custom data: " + piece);
            if (piece.equals("chestplate")) require(output.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM)
                .getFluidInTank(0).getAmount() == 1234, "quantum upgrade keeps tank contents");
            require(ItemStack.matches(before, inputArmor), "recipe preview never mutates source armor");
        }
    }

    private static void dyeAndWash(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var held = player.getMainHandItem();
        var pos = new BlockPos(470, 127, 470);
        var original = level.getBlockState(pos);
        try {
            for (String piece : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
                var armor = (ModularArmor) stack("quantum_suit_" + piece).getItem();
                var shell = armor.defaultArmor();
                armor.setCharge(shell, 54321);
                var dyed = DyedItemColor.applyDyes(shell, java.util.List.of((net.minecraft.world.item.DyeItem) Items.RED_DYE));
                require(dyed.has(DataComponents.DYED_COLOR) && !shell.has(DataComponents.DYED_COLOR), "quantum dye copies the original armor");
                player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
                var state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
                level.setBlockAndUpdate(pos, state);
                state.useItemOn(dyed, level, player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
                require(!dyed.has(DataComponents.DYED_COLOR) && level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL) == 2,
                    "water cauldron washes quantum " + piece + " and spends one water level");
                require(ItemStack.matches(dyed, shell), "washing keeps battery and modules");
            }
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, held); level.setBlockAndUpdate(pos, original); }
    }

    private static void categoryAdmission() {
        var shell = stack("quantum_suit_helmet");
        var armor = (ModularArmor) shell.getItem();
        for (String id : java.util.List.of("protective_module", "lava_module", "extinguish_module")) {
            var module = stack(id);
            require(!armor.install(shell, ArmorModule.HUD, module) && module.getCount() == 1, "universal module cannot occupy HUD: " + id);
        }
    }

    private static void configuratorLayout(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var pos = new BlockPos(470, 126, 470);
        var original = level.getBlockState(pos);
        try {
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:armor_configurator")).orElseThrow().defaultBlockState());
            var menu = ((net.minecraft.world.MenuProvider) level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
            require(menu.slots.size() == 68, "source configurator has armor, 27 module, 36 inventory and four worn armor slots");
            require(!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)) && menu.getSlot(0).mayPlace(stack("nano_suit_helmet")), "configurator accepts only modular armor");
            var plate = stack("carbon_plate");
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                plate, new ItemStack(Items.CRAFTING_TABLE), plate, plate, stack("circuit"), plate,
                ItemStack.EMPTY, stack("machine_block"), ItemStack.EMPTY));
            var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager()
                .byKey(ResourceLocation.parse("ic2:armor_configurator")).orElseThrow().value();
            require(recipe.matches(input, level), "source configurator recipe uses crafting table");
        } finally { level.setBlockAndUpdate(pos, original); }
    }

    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
