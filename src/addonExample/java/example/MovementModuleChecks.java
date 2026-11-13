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
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST);
        var saved = slots.stream().map(s -> player.getItemBySlot(s).copy()).toList(); var position = player.position(); var velocity = player.getDeltaMovement(); boolean ground = player.onGround(), shift = player.isShiftKeyDown(); float fall = player.fallDistance;
        try {
            player.setShiftKeyDown(false); player.setPos(750.5, 100, 750.5);
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
