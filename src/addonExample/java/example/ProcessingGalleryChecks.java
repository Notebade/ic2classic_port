package example;

import ic2.api.Ic2Api;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Every implemented processor, with a persistent numbered gallery for manual review. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class ProcessingGalleryChecks {
    private static final String[] MACHINES = {
        "stone_macerator", "iron_furnace", "macerator", "electric_furnace", "electric_blast_furnace", "electric_smoker",
        "sawmill", "alloy_smelter", "extractor", "compressor", "recycler", "rare_earth_extractor", "uranium_enricher",
        "canner", "vacuum_canner", "electrolyzer", "charged_electrolyzer", "mass_fabricator", "pump",
        "induction_furnace", "induction_blast_furnace", "induction_smoker", "rotary_macerator", "centrifugal_extractor",
        "singularity_compressor", "centrifugal_rare_earth_extractor", "compacting_recycler", "slow_grinder", "pressure_alloy_furnace",
        "wood_gassifier", "electric_wood_gassifier", "refinery"
    };
    private static int age;
    private static boolean finished;
    private static BlockPos position(int i) { return new BlockPos(1000 + i % 4 * 6, 81, 1000 + i / 4 * 6); }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.processing.verify") || finished) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null || client.level == null) return;
        age++;
        if (age == 1) server.execute(() -> prepare(client));
        if (age < 80) return;
        int step = (age - 80) / 30;
        int offset = (age - 80) % 30;
        if (step == MACHINES.length) {
            finished = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(client.player.getUUID());
                player.closeContainer();
                player.connection.teleport(1008, 82, 994, 0, 10);
                player.sendSystemMessage(Component.literal("IC2 GUI: 32 numbered processors. Right-click each machine to inspect. Creative inventory is available."));
            });
            com.mojang.logging.LogUtils.getLogger().info("IC2 processing gallery checks passed: {} machines", MACHINES.length);
            if (!Boolean.getBoolean("ic2.processing.keepOpen")) client.stop();
            return;
        }
        if (offset == 0) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(client.player.getUUID());
            var pos = position(step);
            player.connection.teleport(pos.getX() + .5, 81, pos.getZ() - 2, 0, 0);
            var provider = (MenuProvider) server.overworld().getBlockEntity(pos);
            if (player.openMenu(provider).isEmpty()) throw new AssertionError("Menu refused " + MACHINES[step]);
        });
        if (offset == 10 || offset == 20) {
            if (!(client.screen instanceof AbstractContainerScreen<?> screen)
                    || !(screen.getClass().getSimpleName().equals("ProcessingMachineScreen") || screen.getClass().getSimpleName().equals("AlloySmelterScreen")))
                throw new AssertionError("Wrong processor screen for " + MACHINES[step] + ": " + client.screen);
            if (offset == 10) {
                for (var slot : screen.getMenu().slots) {
                    if (slot.x < 8 || slot.x + 18 > 248 || slot.y < 30 || slot.y + 18 > 238)
                        throw new AssertionError("Slot out of frame: " + MACHINES[step]);
                }
                Screenshot.grab(client.gameDirectory, "processor-" + MACHINES[step] + "-work.png", client.getMainRenderTarget(), ignored -> {});
                var tabs = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.getY() == (screen.height - 240) / 2 + 4)
                    .sorted(Comparator.comparingInt(Button::getX)).toList();
                if (tabs.size() != 4) throw new AssertionError("Missing tabs: " + MACHINES[step]);
                tabs.get(1).onPress();
            } else {
                if (screen.getMenu().slots.stream().filter(slot -> slot.isActive()).count() != 36)
                    throw new AssertionError("Recipe preview has live machine slots: " + MACHINES[step]);
                var search = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
                search.setValue("no_match_expected");
                var create = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(button -> button.getMessage().getString().equals("Создать")).findFirst().orElseThrow();
                if (create.active) throw new AssertionError("Create active for empty filter: " + MACHINES[step]);
                search.setValue("");
                Screenshot.grab(client.gameDirectory, "processor-" + MACHINES[step] + "-recipes.png", client.getMainRenderTarget(), ignored -> {});
            }
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        var player = server.getPlayerList().getPlayer(client.player.getUUID());
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        level.setDayTime(1000);
        for (int x = 997; x <= 1022; x++) for (int z = 994; z <= 1046; z++) {
            level.setBlockAndUpdate(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState());
            for (int y = 81; y < 85; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (int i = 0; i < MACHINES.length; i++) {
            var pos = position(i);
            var state = Ic2Api.block(ResourceLocation.parse("ic2:" + MACHINES[i])).orElseThrow().defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            if (MACHINES[i].equals("pressure_alloy_furnace")) {
                for (int n = 1; n < 8; n++) level.setBlockAndUpdate(pos.offset(-(n % 2), -(n / 4), n / 2 % 2),
                    Ic2Api.block(ResourceLocation.parse("ic2:pressure_alloy_furnace_structure")).orElseThrow().defaultBlockState());
            }
            var signPos = pos.north();
            level.setBlockAndUpdate(signPos, Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 8));
            if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                String id = MACHINES[i];
                var text = new SignText().setMessage(0, Component.literal(String.format("%02d", i + 1)))
                    .setMessage(1, Component.literal(id.substring(0, Math.min(15, id.length()))))
                    .setMessage(2, Component.literal(id.length() > 15 ? id.substring(15, Math.min(30, id.length())) : ""));
                sign.setText(text, true); sign.setText(text, false);
            }
            if (MACHINES[i].equals("electric_furnace") || MACHINES[i].equals("iron_furnace") || MACHINES[i].equals("induction_furnace"))
                verifyTransfer(level, player, pos, MACHINES[i].equals("induction_furnace"));
            verifyRecipeLoading(level, player, pos, MACHINES[i].equals("alloy_smelter"));
        }
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.COPPER_INGOT, 64));
        player.getInventory().setItem(1, new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow(), 64));
        player.getInventory().setItem(2, new ItemStack(Items.RAW_IRON, 64));
        player.getInventory().setItem(3, new ItemStack(Items.COAL, 64));
        player.getInventory().setItem(4, new ItemStack(Items.OAK_LOG, 64));
    }

    private static void verifyTransfer(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, boolean multiple) {
        player.connection.teleport(pos.getX(), 81, pos.getZ() - 2, 0, 0);
        AbstractContainerMenu menu = ((MenuProvider) level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
        int count = menu.slots.size() - 36;
        int input = multiple ? 1 : 0;
        player.getInventory().clearContent();
        var recipe = level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
            .filter(holder -> !holder.value().getIngredients().isEmpty()).sorted(Comparator.comparing(holder -> holder.id().toString())).findFirst().orElseThrow();
        var ingredient = recipe.value().getIngredients().getFirst().getItems()[0].copyWithCount(64);
        menu.getSlot(count).set(ingredient.copy());
        if (multiple) menu.getSlot(count + 1).set(ingredient.copy());
        if (!menu.clickMenuButton(player, 1000) || menu.getSlot(input).getItem().getCount() != 1)
            throw new AssertionError("Single loading failed for " + level.getBlockState(pos).getBlock());
        if (!menu.clickMenuButton(player, 1001) || menu.getSlot(input).getItem().getCount() != 64
                || multiple && menu.getSlot(2).getItem().getCount() != 64)
            throw new AssertionError("Maximum loading failed for " + level.getBlockState(pos).getBlock());
        if (menu.clickMenuButton(player, Integer.MAX_VALUE)) throw new AssertionError("Invalid recipe accepted");
    }

    /** Reflection supplies internal recipe fixtures; assertions exercise actual public server menu operations. */
    private static void verifyRecipeLoading(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, boolean alloy) {
        try {
            player.connection.teleport(pos.getX(), 81, pos.getZ() - 2, 0, 0);
            var menu = ((MenuProvider) level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
            var contract = Class.forName("ic2.core.machine.ProcessingMenu");
            var viewType = Class.forName("ic2.core.machine.MachineRecipeView");
            var viewsMethod = contract.getDeclaredMethod("recipeViews", net.minecraft.world.level.Level.class);
            viewsMethod.setAccessible(true);
            var targetsMethod = contract.getDeclaredMethod("recipeInputs", viewType);
            targetsMethod.setAccessible(true);
            var views = (java.util.List<?>) viewsMethod.invoke(menu, level);
            int machineSlots = menu.slots.size() - 36;
            boolean itemRecipeExists = false;
            for (int index = 0; index < views.size(); index++) {
                var recipe = views.get(index);
                var inputs = (java.util.List<?>) value(recipe, "inputs");
                if (inputs.isEmpty()) continue;
                var targets = (java.util.List<?>) targetsMethod.invoke(menu, recipe);
                if (inputs.size() != targets.size()) continue;
                var ingredients = new java.util.ArrayList<ItemStack>();
                for (Object input : inputs) {
                    var fluid = (net.neoforged.neoforge.fluids.FluidStack) value(input, "fluid");
                    var items = (java.util.List<?>) value(input, "items");
                    if (!fluid.isEmpty() || items.isEmpty()) break;
                    ingredients.add(((ItemStack) items.getFirst()).copy());
                }
                if (ingredients.size() != inputs.size()) continue;
                itemRecipeExists = true;
                player.getInventory().clearContent();
                for (int i = 0; i < machineSlots; i++) menu.getSlot(i).set(ItemStack.EMPTY);
                for (int i = 0; i < ingredients.size(); i++) menu.getSlot(machineSlots + i).set(ingredients.get(i).copyWithCount(ingredients.get(i).getMaxStackSize()));
                int before = menu.slots.stream().mapToInt(slot -> slot.getItem().getCount()).sum();
                int button = alloy ? index : 1000 + index * 2;
                if (!menu.clickMenuButton(player, button)) continue;
                for (int i = 0; i < ingredients.size(); i++)
                    if (menu.getSlot((Integer) targets.get(i)).getItem().getCount() != ingredients.get(i).getCount())
                        throw new AssertionError("Incorrect single batch: " + level.getBlockState(pos).getBlock());
                if (menu.slots.stream().mapToInt(slot -> slot.getItem().getCount()).sum() != before)
                    throw new AssertionError("Single load duplicated/lost items");
                int maximum = alloy ? index + views.size() : 1001 + index * 2;
                if (!menu.clickMenuButton(player, maximum)) throw new AssertionError("Maximum batch refused");
                if (menu.slots.stream().mapToInt(slot -> slot.getItem().getCount()).sum() != before)
                    throw new AssertionError("Maximum load duplicated/lost items");
                com.mojang.logging.LogUtils.getLogger().info("IC2 processor transfer passed: {}", level.getBlockState(pos).getBlock());
                return;
            }
            if (itemRecipeExists) throw new AssertionError("No item recipe could load: " + level.getBlockState(pos).getBlock());
            if (!views.isEmpty() && menu.clickMenuButton(player, 1000)) throw new AssertionError("Unsupported fluid transfer accepted");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Recipe fixture lookup failed", error);
        }
    }

    private static Object value(Object record, String accessor) throws ReflectiveOperationException {
        var method = record.getClass().getDeclaredMethod(accessor);
        method.setAccessible(true);
        return method.invoke(record);
    }
}
