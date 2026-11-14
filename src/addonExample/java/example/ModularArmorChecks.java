package example;

import ic2.api.Ic2Api;
import ic2.api.item.ArmorModule;
import ic2.api.item.ElectricItem;
import ic2.api.item.ModularArmor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

final class ModularArmorChecks {
    static void run(ServerLevel level) throws Exception {
        checkSonar(level);
        for (String piece : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
            var shell = stack("nano_suit_" + piece); var armor = (ModularArmor) shell.getItem();
            require(!ElectricItem.supports(shell, Integer.MAX_VALUE, true), "bare nano shell has no installed energy store");
            var standard = armor.defaultArmor();
            require(armor.capacity(standard) == 100000 && armor.transferRate(standard) == 250 && armor.minimumPacket(standard) == 128 && !armor.canProvideEnergy(standard), "default source nano battery profile");
            require(armor.modules(standard).size() == 1 && armor.modules(standard).getFirst().stack().is(stack("energy_crystal").getItem()), "default armor installs the source energy crystal");
            var rejected = stack("re_battery");
            require(!armor.install(shell, ArmorModule.BATTERY, rejected) && rejected.getCount() == 1, "RE battery is not a source armor module");
            var cell = stack("energy_crystal"); var power = (ElectricItem) cell.getItem(); power.setCharge(cell, 1234);
            require(armor.canInstall(shell, ArmorModule.BATTERY, cell) && armor.charge(shell) == 0 && armor.modules(shell).isEmpty(), "module admission simulation has no side effects");
            require(armor.install(shell, ArmorModule.BATTERY, cell) && cell.isEmpty() && armor.charge(shell) == 1234, "install takes custody of one charged battery without duplicating EU");
            require(power.charge(armor.modules(shell).getFirst().stack()) == 0, "installed battery does not retain a duplicate charge");
            var preview = armor.removeModule(shell, 0, true);
            require(power.charge(preview) == 1234 && armor.charge(shell) == 1234 && armor.modules(shell).size() == 1, "removal simulation previews charge without mutation");
            var removed = armor.removeModule(shell, 0, false);
            require(power.charge(removed) == 1234 && armor.capacity(shell) == 0 && armor.charge(shell) == 0 && armor.modules(shell).isEmpty(), "removal restores stored EU to the battery and removes its modifiers");
            var detached = armor.modules(standard).getFirst().stack(); detached.shrink(1);
            require(armor.modules(standard).getFirst().stack().getCount() == 1, "module views are detached");
            require(armor.setEnhanced(standard, true) && armor.enhanced(standard), "nano upgrade persists source quantum-level slot limits");
        }
        checkAddonModule(level);
        checkProtection(level);
        checkCrafting(level);
        PassiveModuleChecks.run(level);
        NightVisionChecks.run(level);
        MovementModuleChecks.run(level);
        WaterWalkerChecks.run(level);
        FallArmorChecks.run(level);
        ElectricArmorChecks.run(level);
    }
    static void checkSonar(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var position = player.position();
        var slot = net.minecraft.world.entity.EquipmentSlot.HEAD;
        var previous = player.getItemBySlot(slot);
        boolean sneaking = player.isShiftKeyDown();
        var helmet = stack("nano_suit_helmet");
        var armor = (ModularArmor) helmet.getItem();
        helmet = armor.defaultArmor();
        var cow = net.minecraft.world.entity.EntityType.COW.create(level);
        try {
            require(armor.install(helmet, ArmorModule.HUD, stack("sonar_module")), "sonar uses the source HUD slot");
            armor.setCharge(helmet, 20_000);
            player.setPos(470.5, 138, 470.5);
            player.setShiftKeyDown(false);
            player.setItemSlot(slot, helmet);
            cow.setPos(473.5, 138, 470.5);
            require(level.addFreshEntity(cow), "sonar fixture spawns");
            ic2.api.item.EquipmentControls.update(player, new ic2.api.item.EquipmentInput(false, false, false, true, false, true));
            ((ic2.api.item.ArmorTickItem) helmet.getItem()).tickArmor(helmet, player, slot);
            require(!cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && armor.charge(helmet) == 0,
                "Alt+H starts the source wave for 20000 EU without immediately revealing distant mobs");
            ic2.api.item.EquipmentControls.clear(player);
            for (int tick = 0; tick < 4; tick++) ((ic2.api.item.ArmorTickItem) helmet.getItem()).tickArmor(helmet, player, slot);
            require(cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && armor.charge(helmet) == 0,
                "sonar wave reaches the mob as its radius expands without further EU cost");
        } finally {
            cow.discard();
            player.setPos(position);
            player.setItemSlot(slot, previous);
            player.setShiftKeyDown(sneaking);
            ic2.api.item.EquipmentControls.clear(player);
        }
    }
    private static void checkCrafting(ServerLevel level) {
        var cell = stack("energy_crystal"); ((ElectricItem) cell.getItem()).setCharge(cell, 4321);
        var plate = stack("carbon_plate");
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(plate, cell, plate, plate, new ItemStack(net.minecraft.world.item.Items.GLASS), plate));
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:normal_nano_armor_helmet")).orElseThrow(() -> new AssertionError("source nano helmet recipe exists"));
        require(((net.minecraft.world.item.crafting.CraftingRecipe) recipe.value()).matches(input, level), "source nano helmet ingredient pattern matches");
        var result = ((net.minecraft.world.item.crafting.CraftingRecipe) recipe.value()).assemble(input, level.registryAccess());
        var armor = (ModularArmor) result.getItem();
        require(armor.modules(result).size() == 1 && armor.charge(result) == 4321 && ((ElectricItem) cell.getItem()).charge(cell) == 4321, "normal recipe installs battery and carries ingredient EU without changing input");
        var upgrade = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:upgraded_nano_armor_helmet")).orElseThrow();
        var upgradedInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(ItemStack.EMPTY, stack("plate_iridium"), ItemStack.EMPTY, ItemStack.EMPTY, result, ItemStack.EMPTY, stack("complex_circuit"), ItemStack.EMPTY, stack("complex_circuit")));
        var upgraded = ((net.minecraft.world.item.crafting.CraftingRecipe) upgrade.value()).assemble(upgradedInput, level.registryAccess());
        require(armor.enhanced(upgraded) && armor.charge(upgraded) == 4321 && armor.modules(upgraded).size() == 1 && !armor.enhanced(result), "source enhanced crafting copies complete armor state without mutating its input");
    }
    private static void checkAddonModule(ServerLevel level) {
        var item = (ModularArmor) stack("nano_suit_helmet").getItem(); var armor = item.defaultArmor();
        var modules = new ItemStack(ExampleAddon.ARMOR_TEST_MODULE.get(), 3);
        require(item.install(armor, ArmorModule.GENERIC, modules) && item.install(armor, ArmorModule.GENERIC, modules), "native armor installs a separately compiled addon module");
        require(item.modules(armor).get(1).stack().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("installed") == 1, "addon installation callback changes persist on the owned module");
        require(!item.install(armor, ArmorModule.GENERIC, modules) && modules.getCount() == 1, "source nano generic slot limit rejects a third module without consuming it");
        require(item.capacity(armor) == 102000 && item.transferRate(armor) == 270 && item.canProvideEnergy(armor) && item.features(armor).contains(ResourceLocation.parse("exampleaddon:fixture")), "addon modifiers change actual per-stack armor storage and provider state");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level); var saved = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy();
        try {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, armor);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            var data = armor.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            require(data.getInt("module_installs") == 2 && data.getInt("module_ticks") == 2, "addon lifecycle and real worn module ticks are called exactly once");
            require(item.modules(armor).get(1).stack().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("ticks") == 1, "addon tick callback state persists on its owned module");
        } finally { player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, saved); }
        require(item.setEnhanced(armor, true) && item.install(armor, ArmorModule.GENERIC, modules), "enhanced nano helmet accepts the third source generic slot");
        require(!item.setEnhanced(armor, false) && item.enhanced(armor), "downgrade cannot discard installed modules");
        var removed = item.removeModule(armor, 1, false);
        require(removed.is(ExampleAddon.ARMOR_TEST_MODULE.get()) && item.capacity(armor) == 102000, "addon removal updates cached power modifiers");
        require(removed.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("removed") == 1, "addon removal callback changes persist on the returned item");
        var restored = ItemStack.parse(level.registryAccess(), armor.save(level.registryAccess())).orElseThrow();
        require(item.capacity(restored) == 102000 && item.enhanced(restored) && item.modules(restored).size() == 3 && item.features(restored).equals(item.features(armor)), "module custody, addon modifiers and enhancement survive real stack serialization");
        item.setCharge(armor, 102000);
        require(item.removeModule(armor, 1, false).isEmpty() && item.charge(armor) == 102000 && item.modules(armor).size() == 3, "capacity removal rejects excess EU without losing charge or a module");
    }
    private static void checkProtection(ServerLevel level) {
        var protectedMob = mob(level, true); float health = protectedMob.getHealth();
        protectedMob.hurt(protectedMob.damageSources().mobAttack(net.minecraft.world.entity.EntityType.COW.create(level)), 10);
        require(protectedMob.getHealth() == health, "full charged source nano set absorbs a normal hit");
        int remaining = 0;
        for (var armor : protectedMob.getArmorSlots()) remaining += ((ElectricItem) armor.getItem()).charge(armor);
        require(remaining == 393600, "source normalized ratios and integer wear spend exactly 6400 EU for ten damage");
        var magic = mob(level, true); health = magic.getHealth(); magic.hurt(magic.damageSources().magic(), 10);
        require(magic.getHealth() == health - 10, "source magical armor-bypassing damage penetrates nano protection");
        for (var armor : magic.getArmorSlots()) require(((ElectricItem) armor.getItem()).charge(armor) == 100000, "excluded magic consumes no armor EU");
        var empty = mob(level, false);
        require(empty.getArmorValue() == 20 && empty.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS) == 8, "source diamond-material physical armor remains without power");
        health = empty.getHealth(); empty.hurt(empty.damageSources().mobAttack(net.minecraft.world.entity.EntityType.COW.create(level)), 10);
        require(Math.abs(empty.getHealth() - (health - 3)) < .001, "empty source shells retain native physical damage reduction");
    }
    private static net.minecraft.world.entity.animal.Cow mob(ServerLevel level, boolean charged) {
        var mob = net.minecraft.world.entity.EntityType.COW.create(level); mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(700.5, 100, 700.5);
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        for (String piece : java.util.List.of("helmet", "chestplate", "leggings", "boots")) {
            var shell = stack("nano_suit_" + piece); var item = (ModularArmor) shell.getItem(); var armor = charged ? item.defaultArmor() : shell;
            if (charged) item.setCharge(armor, 100000); mob.setItemSlot(item.equipmentSlot(), armor);
        }
        mob.tick(); mob.setHealth(200); return mob;
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source modular armor exists: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
