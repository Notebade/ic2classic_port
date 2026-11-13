package example;

import ic2.api.Ic2Api;
import ic2.api.item.Toolbox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ToolboxChecks {
    static void run(ServerLevel level) {
        var ids = java.util.List.of("toolbox", "carbon_box", "iridium_box");
        int[] capacities = {8, 15, 45};
        for (int i = 0; i < ids.size(); i++) {
            var stack = stack(ids.get(i));
            require(stack.getItem() instanceof Toolbox, "native toolbox exposes addon storage contract");
            var box = (Toolbox) stack.getItem();
            require(box.slots() == capacities[i] && !box.accepts(new ItemStack(Items.DIRT)) && !box.accepts(stack), "source toolbox capacity and tool-only filtering");
            require(box.store(stack, capacities[i] - 1, new ItemStack(Items.IRON_PICKAXE)), "last source slot stores a tool");
            require(!box.store(stack, capacities[i], new ItemStack(Items.IRON_PICKAXE)) && !box.store(stack, 0, new ItemStack(Items.DIAMOND)), "reject out-of-range and material writes");
            var detached = box.stored(stack, capacities[i] - 1); detached.shrink(1);
            require(box.stored(stack.copy(), capacities[i] - 1).is(Items.IRON_PICKAXE), "contents persist and API reads are detached");
            checkMenu(level, stack, box);
            if (i > 0) {
                var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
                var oldOffhand = player.getOffhandItem().copy(); var oldMain = player.getMainHandItem().copy();
                try {
                    player.setItemInHand(InteractionHand.OFF_HAND, stack); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                    require(box.scroll(stack, player, 1) && player.getMainHandItem().is(Items.IRON_PICKAXE), "extended toolbox scroll swaps a held tool");
                    require(box.stored(stack, capacities[i] - 1).is(Items.IRON_AXE), "scroll conserves the previous held tool");
                    require(!box.scroll(stack.copy(), player, 1), "detached unowned box cannot modify player inventory");
                } finally { player.setItemInHand(InteractionHand.MAIN_HAND, oldMain); player.setItemInHand(InteractionHand.OFF_HAND, oldOffhand); }
            }
        }
        var source = stack("toolbox");
        var drill = stack("diamond_drill"); ((ic2.api.item.ElectricItem) drill.getItem()).setCharge(drill, 4321);
        ((Toolbox) source.getItem()).store(source, 7, drill);
        for (String plate : java.util.List.of("carbon_plate", "plate_iridium")) {
            var grid = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(6, stack(plate)));
            grid.set(1, source);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, grid);
            var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow(() -> new AssertionError("source inventory-preserving toolbox upgrade recipe exists"));
            var result = recipe.value().assemble(input, level.registryAccess());
            require(result.getItem() instanceof Toolbox && ((Toolbox) result.getItem()).slots() == (plate.equals("carbon_plate") ? 15 : 45), "source toolbox upgrade result");
            var kept = ((Toolbox) result.getItem()).stored(result, 7);
            require(kept.is(drill.getItem()) && ((ic2.api.item.ElectricItem) kept.getItem()).charge(kept) == 4321, "toolbox upgrade preserves tool and charge");
            require(((Toolbox) source.getItem()).stored(source, 7).is(drill.getItem()), "upgrade assembly does not mutate input");
            source = result;
        }
    }
    private static void checkMenu(ServerLevel level, ItemStack stack, Toolbox box) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ToolboxMenuTest")) {
            @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider) {
                containerMenu = provider.createMenu(43, getInventory(), this); return java.util.OptionalInt.of(43);
            }
        };
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        stack.use(level, player, InteractionHand.OFF_HAND);
        int physical = box.slots() == 8 ? 9 : box.slots() == 15 ? 18 : 45;
        var menu = player.containerMenu;
        require(menu.slots.size() == physical + 36 && !menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "backend menu exposes source capacity and applies tool filtering");
        if (physical > box.slots()) require(!menu.getSlot(box.slots()).mayPlace(new ItemStack(Items.IRON_PICKAXE)), "padding slots remain inaccessible");
        menu.quickMoveStack(player, physical + 27);
        require(player.getMainHandItem().isEmpty() && box.stored(stack, 0).is(Items.IRON_SWORD), "real shift-click stores and persists a tool");
        menu.quickMoveStack(player, 0);
        require(box.stored(stack, 0).isEmpty() && player.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "real shift-click extracts the same tool");
        player.closeContainer();
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source toolbox exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
