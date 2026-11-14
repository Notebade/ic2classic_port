package ic2.core.equipment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Equipment {
    private Equipment() {}
    public static void register(IEventBus bus, DeferredRegister.Items items) {
        bus.addListener(EquipmentInputPayload::register);
        ConventionalTools.register(items);
        ConventionalArmor.register(bus, items);
        WearableBatteries.register(items);
        SolarArmor.register(items);
        StaticBoots.register(items);
        ArmorConfiguration.register(bus);
        ModularSuitItem.register(items);
        PassiveArmorModule.register(items);
        items.registerItem("sonar_module", SonarModule::new);
        FoodCans.register(items);
        items.registerItem("food_storage_module", FoodStorageModule::new);
        FoodStorageMenu.register(bus);
        NightVisionGoggles.register(items);
        MovementArmorModule.register(items);
        CarbonBoats.register(bus, items);
        EquipmentMaterials.register(bus);
        ConventionalShield.register(items);
        items.registerItem("nano_shield", NanoShield::new);
        NanoSaber.register(bus, items);
        MiningLaser.register(bus, items);
        ConstructionFoam.register(bus, items);
        PoweredDrill.register(items);
        items.registerItem("electric_hoe", p -> new PoweredHoe(p, false));
        items.registerItem("advanced_hoe", p -> new PoweredHoe(p, true));
        items.registerItem("chainsaw", p -> new PoweredChainsaw(p, false));
        items.registerItem("advanced_chainsaw", p -> new PoweredChainsaw(p, true));
        items.registerItem("mowing_tool", PoweredMower::new);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, PoweredMower::collectDrops);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Equipment::tickArmor);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ArmorFalls::fall);
    }
    private static void tickArmor(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide || !player.isAlive()) return;
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            var stack = player.getItemBySlot(slot);
            if (stack.getCount() == 1 && stack.getItem() instanceof ic2.api.item.ArmorTickItem item) item.tickArmor(stack, player, slot);
        }
        MovementArmorModule.refreshStep(player);
    }
}
