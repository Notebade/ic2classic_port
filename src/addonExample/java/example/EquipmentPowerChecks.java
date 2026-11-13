package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

final class EquipmentPowerChecks {
    static void run(ServerLevel level) {
        var ids = java.util.List.of("batpack", "lappack", "quantum_pack", "industrial_belt");
        int[] capacities = {60000, 300000, 1200000, 30000}, rates = {100, 250, 1000, 100}, packets = {32, 128, 512, 32};
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.MAINHAND);
        var saved = slots.stream().map(slot -> player.getItemBySlot(slot).copy()).toList();
        try {
            for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
            for (int i = 0; i < ids.size(); i++) {
                var pack = stack(ids.get(i)); var packItem = pack.getItem(); var power = (ElectricItem) packItem;
                require(pack.getItem() instanceof ArmorItem && power.canProvideEnergy() && power.capacity() == capacities[i] && power.transferRate() == rates[i] && power.minimumPacket() == packets[i], "source wearable energy catalog: " + ids.get(i));
                var slot = i == 3 ? EquipmentSlot.LEGS : EquipmentSlot.CHEST;
                power.setCharge(pack, 1000); player.setItemInHand(InteractionHand.MAIN_HAND, pack);
                pack.use(level, player, InteractionHand.MAIN_HAND);
                var worn = player.getItemBySlot(slot);
                require(worn.is(packItem) && ((ArmorItem) worn.getItem()).getDefense() == 0, "battery equipment actually equips without passive armor protection");
                var drill = stack("diamond_drill"); var toolPower = (ElectricItem) drill.getItem();
                player.setItemInHand(InteractionHand.MAIN_HAND, drill);
                drill.getItem().mineBlock(drill, level, Blocks.STONE.defaultBlockState(), BlockPos.ZERO, player);
                require(toolPower.charge(drill) == 920 && power.charge(worn) == 0, "native tool draws worn energy and spends source 80 EU");
                power.setCharge(worn, 20000); toolPower.setCharge(drill, 100);
                drill.getItem().mineBlock(drill, level, Blocks.STONE.defaultBlockState(), BlockPos.ZERO, player);
                require(toolPower.charge(drill) == 10000 && power.charge(worn) == 10020, "source armor charging fills before and after use and conserves EU");
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            var enchanted = stack("drill"); var power = (ElectricItem) enchanted.getItem();
            var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            enchanted.enchant(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 2);
            enchanted.enchant(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
            var drill = (ic2.api.item.MiningDrill) enchanted.getItem();
            power.setCharge(enchanted, 45);
            require(drill.canUse(enchanted), "native drill uses source enchantment-adjusted affordability");
            power.setCharge(enchanted, 100); drill.consumeUse(enchanted);
            require(power.charge(enchanted) == 55, "native drill consumes source 45 EU with efficiency II and unbreaking III");
            power.setCharge(enchanted, 44); drill.consumeUse(enchanted);
            require(power.charge(enchanted) == 44 && !drill.canUse(enchanted), "unaffordable use never discards a partial charge");
            var cell = stack("re_battery"); ((ElectricItem) cell.getItem()).setCharge(cell, 100);
            var grid = java.util.List.of(cell, stack("circuit"), cell, cell, stack("ingot_tin"), cell, cell, ItemStack.EMPTY, cell);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
            var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow(() -> new AssertionError("source batpack recipe preserves input EU"));
            var crafted = recipe.value().assemble(input, level.registryAccess());
            require(crafted.is(stack("batpack").getItem()) && ((ElectricItem) crafted.getItem()).charge(crafted) == 600, "batpack crafting transfers all six battery charges");
            require(((ElectricItem) cell.getItem()).charge(cell) == 100, "recipe assembly never drains input batteries");
            var menu = new net.minecraft.world.inventory.CraftingMenu(78, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, BlockPos.ZERO));
            for (int slot = 0; slot < grid.size(); slot++) menu.getSlot(slot + 1).set(grid.get(slot).copy());
            menu.slotsChanged(menu.getSlot(1).container);
            var taken = menu.getSlot(0).remove(1); menu.getSlot(0).onTake(player, taken);
            require(((ElectricItem) taken.getItem()).charge(taken) == 600, "real crafting-menu take retains the six cell charges");
            for (int slot = 1; slot <= 9; slot++) require(menu.getSlot(slot).getItem().isEmpty(), "real pack craft consumes all input cells and materials");
        } finally { for (int i = 0; i < slots.size(); i++) player.setItemSlot(slots.get(i), saved.get(i)); }
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source wearable energy item exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
