package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

final class PassiveModuleChecks {
    static void run(ServerLevel level) {
        checkBeltRecovery(level);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var savedHead = player.getItemBySlot(EquipmentSlot.HEAD).copy(); var savedChest = player.getItemBySlot(EquipmentSlot.CHEST).copy();
        var air = player.getAirSupply(); int fire = player.getRemainingFireTicks();
        try {
            var shell = stack("nano_suit_helmet"); var item = (ModularArmor) shell.getItem(); var armor = item.defaultArmor();
            var charger = stack("charger_module");
            require(item.install(armor, ArmorModule.CHARGER, charger) && item.canProvideEnergy(armor), "charger module enables actual armor EU output");
            require(item.install(armor, ArmorModule.GENERIC, stack("air_refill_module")), "air module installs in source helmet slot");
            player.setItemSlot(EquipmentSlot.HEAD, armor); item.setCharge(armor, 1000); player.setAirSupply(50); tick(player);
            require(player.getAirSupply() == 50 && item.charge(armor) == 1000, "insufficient air refill EU cannot produce partial-price air");
            item.setCharge(armor, 2000); tick(player);
            require(player.getAirSupply() == 250 && item.charge(armor) == 0, "source air refill adds 200 air for 2000 EU");
            require(item.install(armor, ArmorModule.GENERIC, stack("protective_module")), "protective module fits a generic helmet slot");
            item.setCharge(armor, 45000);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200, 1));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200, 0)); tick(player);
            require(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON) && !player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER) && item.charge(armor) == 0, "protection removes poison and wither for source amplifier-scaled EU costs");
            var chest = stack("nano_suit_chestplate"); var chestItem = (ModularArmor) chest.getItem();
            require(!chestItem.install(chest, ArmorModule.BACK, stack("air_refill_module")), "air module rejects incompatible armor and category");
            require(chestItem.install(chest, ArmorModule.BACK, stack("extinguish_module")), "ANY extinguishing module occupies a selected compatible slot");
            player.setItemSlot(EquipmentSlot.CHEST, chest); player.setRemainingFireTicks(100); tick(player);
            require(player.getRemainingFireTicks() <= 0 && chestItem.charge(chest) == 0, "source extinguishing module works without consuming EU");
            var gold = item.defaultArmor(); require(item.install(gold, ArmorModule.GENERIC, stack("golden_module")), "gold module installs");
            require(gold.getItem().makesPiglinsNeutral(gold, player), "gold module provides real piglin-neutral equipment behavior");
        } finally {
            player.setItemSlot(EquipmentSlot.HEAD, savedHead); player.setItemSlot(EquipmentSlot.CHEST, savedChest); player.setAirSupply(air); player.setRemainingFireTicks(fire);
            player.removeEffect(net.minecraft.world.effect.MobEffects.POISON); player.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
        }
    }
    private static void checkBeltRecovery(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level); var inventory = player.getInventory();
        var saved = java.util.stream.IntStream.range(0, inventory.getContainerSize()).mapToObj(i -> inventory.getItem(i).copy()).toList();
        try {
            inventory.clearContent();
            var menu = new net.minecraft.world.inventory.CraftingMenu(79, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(level, net.minecraft.core.BlockPos.ZERO));
            var belt = stack("industrial_belt"); ((ElectricItem) belt.getItem()).setCharge(belt, 30000);
            menu.getSlot(1).set(belt); menu.slotsChanged(menu.getSlot(1).container);
            require(menu.getSlot(0).getItem().is(stack("re_battery").getItem()) && menu.getSlot(0).getItem().getCount() == 3, "source belt recovery shows a three-battery crafting batch");
            menu.quickMoveStack(player, 0);
            int count = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var battery = inventory.getItem(i);
                if (!battery.is(stack("re_battery").getItem())) continue;
                require(battery.getCount() == 1 && ((ElectricItem) battery.getItem()).charge(battery) == 0, "native inventory splits recovered batteries into valid uncharged individual stacks"); count++;
            }
            require(count == 3 && menu.getSlot(1).getItem().isEmpty(), "real Shift craft consumes one belt and returns exactly three batteries");
        } finally { for (int i = 0; i < saved.size(); i++) inventory.setItem(i, saved.get(i)); }
    }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow(() -> new AssertionError("source armor module exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
