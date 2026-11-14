package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class MovementModuleChecks {
    static void run(ServerLevel level) {
        JetpackChecks.run(level);
        for (var id : java.util.List.of("nuclear_jetpack", "compacted_nuclear_jetpack", "compacted_electric_jetpack", "compact_fuel_jetpack")) {
            require(!stack(id).isEmpty() && level.getRecipeManager().byKey(ResourceLocation.parse("ic2:" + id)).isPresent(),
                id + " and recipe load in Minecraft");
        }
        var nuclear = stack("nuclear_jetpack");
        nuclear.set(net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(stack("uranium_rod_single"))));
        var cycle = ic2.core.machine.PortableReactor.cycle(nuclear, 0, level.random);
        require(cycle.output() > 0 && cycle.heat() > 0, "portable reactor produces power and heat from a uranium rod");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST);
        var saved = slots.stream().map(s -> player.getItemBySlot(s).copy()).toList(); var position = player.position(); var velocity = player.getDeltaMovement(); boolean ground = player.onGround(), shift = player.isShiftKeyDown(); float fall = player.fallDistance;
        try {
            player.setShiftKeyDown(false); player.setPos(750.5, 100, 750.5);
            var jetpack = new ItemStack(ic2.api.Ic2Api.item(net.minecraft.resources.ResourceLocation.parse("ic2:electric_jetpack")).orElseThrow());
            var electric = (ElectricItem) jetpack.getItem();
            player.setItemSlot(EquipmentSlot.CHEST, jetpack);
            player.setDeltaMovement(Vec3.ZERO);
            EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false));
            tick(player);
            require(player.getDeltaMovement().y == 0, "empty jetpack provides no thrust");
            electric.setCharge(jetpack, 3000);
            tick(player);
            require(player.getDeltaMovement().y > .13 && electric.charge(jetpack) == 2993, "electric jetpack applies thrust for 7 EU");
            EquipmentControls.clear(player);
            tick(player);
            require(electric.charge(jetpack) == 2993, "jetpack stops after releasing jump");
            require(((ConfigurableTool) jetpack.getItem()).setMode(jetpack, "hover"), "jetpack hover mode is available");
            player.setOnGround(false);
            player.setDeltaMovement(0, -.3, 0);
            tick(player);
            require(Math.abs(player.getDeltaMovement().y + .2) < .0001 && electric.charge(jetpack) == 2989,
                "hover slows descent without jump input for 4 EU");
            var fuelPack = stack("fuel_jetpack");
            var tank = fuelPack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            var execute = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
            var biofuel = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse("ic2:bio_fuel"));
            require(tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), execute) == 0,
                "fuel jetpack rejects water");
            require(tank.fill(new net.neoforged.neoforge.fluids.FluidStack(biofuel, 19000), execute) == 18000, "fuel jetpack stores 18000 mB");
            player.setItemSlot(EquipmentSlot.CHEST, fuelPack);
            player.setDeltaMovement(Vec3.ZERO);
            EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false));
            tick(player);
            require(player.getDeltaMovement().y > .19 && tank.getFluidInTank(0).getAmount() == 17991, "fuel jetpack lifts for 9 mB");
            for (var variant : java.util.Map.of("nuclear_jetpack", 12, "compacted_nuclear_jetpack", 40, "compacted_electric_jetpack", 30).entrySet()) {
                var pack = stack(variant.getKey());
                var power = (ElectricItem) pack.getItem();
                power.setCharge(pack, power.capacity());
                player.setItemSlot(EquipmentSlot.CHEST, pack);
                player.setDeltaMovement(Vec3.ZERO);
                tick(player);
                require(player.getDeltaMovement().y > .18 && power.charge(pack) == power.capacity() - variant.getValue(),
                    variant.getKey() + " lifts and spends configured EU");
            }
            var compactFuel = stack("compact_fuel_jetpack");
            var compactTank = compactFuel.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            require(compactTank.fill(new net.neoforged.neoforge.fluids.FluidStack(biofuel, 1_100_000), execute) == 1_080_118,
                "compact fuel tank reaches its source capacity");
            player.setItemSlot(EquipmentSlot.CHEST, compactFuel);
            player.setDeltaMovement(Vec3.ZERO);
            tick(player);
            require(player.getDeltaMovement().y > .23 && compactTank.getFluidInTank(0).getAmount() == 1_080_083,
                "compact fuel pack lifts for 35 mB");
            EquipmentControls.clear(player);
            for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
            var boots = armor("nano_suit_boots"); var item = (ModularArmor) boots.getItem();
            require(item.install(boots, ArmorModule.MOVEMENT, stack("jump_boost_module")), "jump module fits boots"); item.setCharge(boots, 1000); player.setItemSlot(EquipmentSlot.FEET, boots);
            player.setOnGround(true); EquipmentControls.clear(player); tick(player);
            require(item.charge(boots) == 0, "source jump reserve charges once on ground for 1000 EU"); tick(player); require(item.charge(boots) == 0, "full jump reserve does not recharge twice");
            player.setOnGround(false); player.setDeltaMovement(1, .2, 1); EquipmentControls.update(player, new EquipmentInput(true, true, false, false, false, false)); tick(player);
            require(Math.abs(player.getDeltaMovement().x - 1.75) < .0001 && Math.abs(player.getDeltaMovement().y - .35) < .0001, "source default boosted jump applies stored upward reserve and initial horizontal multiplier");
            require(item.install(boots, ArmorModule.MOVEMENT, stack("step_assist_module")), "step module shares the second boot movement slot"); tick(player);
            require(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) - 1.2) < .0001, "step module changes the real native step attribute");
            item.removeModule(boots, 2, false); tick(player);
            require(Math.abs(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) - .6) < .0001, "removing a worn step module revokes its attribute");
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            var legs = armor("nano_suit_leggings"); var legItem = (ModularArmor) legs.getItem();
            require(legItem.install(legs, ArmorModule.MOVEMENT, stack("speed_boost_module")), "speed module fits leggings"); legItem.setCharge(legs, 10000); player.setItemSlot(EquipmentSlot.LEGS, legs);
            player.setOnGround(true); player.setDeltaMovement(Vec3.ZERO); EquipmentControls.update(player, new EquipmentInput(false, true, true, false, false, false));
            for (int i = 0; i < 10; i++) tick(player);
            require(legItem.charge(legs) == 9000 && player.getDeltaMovement().horizontalDistance() > 2, "source ground speed adds movement and spends 1000 EU per ten active ticks");
            player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
            var chest = armor("nano_suit_chestplate"); var chestItem = (ModularArmor) chest.getItem();
            require(chestItem.install(chest, ArmorModule.BACK, stack("parachute_module")), "parachute occupies the back slot"); player.setItemSlot(EquipmentSlot.CHEST, chest);
            player.setOnGround(false); player.setShiftKeyDown(true); player.setDeltaMovement(0, -1, 0); player.fallDistance = 20;
            EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false)); tick(player); tick(player);
            require(player.getDeltaMovement().y >= -.2 && player.fallDistance == 0, "source parachute limits descent and resets accumulated fall distance");
            player.setOnGround(true); tick(player); player.setOnGround(false); player.setShiftKeyDown(false); EquipmentControls.clear(player); player.setDeltaMovement(0, -1, 0); tick(player);
            require(player.getDeltaMovement().y == -1, "landing disables parachute until activated again");
        } finally {
            for (int i = 0; i < slots.size(); i++) player.setItemSlot(slots.get(i), saved.get(i)); EquipmentControls.clear(player);
            player.setPos(position); player.setDeltaMovement(velocity); player.setOnGround(ground); player.setShiftKeyDown(shift); player.fallDistance = fall; tick(player);
        }
    }
    private static ItemStack armor(String id) { var shell = stack(id); return ((ModularArmor) shell.getItem()).defaultArmor(); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow(() -> new AssertionError("source movement module exists: " + id))); }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
