package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class NanoSaberChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.saber.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused nanosaber checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous = player.getMainHandItem(); var oldChest = player.getItemBySlot(EquipmentSlot.CHEST);
        var saber = stack("nano_saber"); var power = (ElectricItem) saber.getItem();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, saber); EquipmentControls.clear(player);
            require(!saber.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "empty saber cannot activate");
            power.setCharge(saber, 1000); saber.use(level, player, InteractionHand.MAIN_HAND);
            require(saber.canPerformAction(ItemAbilities.SWORD_SWEEP), "right click activates saber");
            require(!saber.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && saber.canPerformAction(ItemAbilities.SWORD_SWEEP),
                "plain right click keeps an active saber on; source requires Alt to turn off");
            EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, true));
            require(saber.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction() && !saber.canPerformAction(ItemAbilities.SWORD_SWEEP), "Alt+right click turns saber off");
            EquipmentControls.clear(player); power.setCharge(saber, 16); saber.use(level, player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 16; tick++) saber.inventoryTick(level, player, 0, true);
            require(power.charge(saber) == 0 && saber.canPerformAction(ItemAbilities.SWORD_SWEEP), "successful final drain keeps saber active until the next failed drain");
            for (int tick = 0; tick < 16; tick++) saber.inventoryTick(level, player, 0, true);
            require(!saber.canPerformAction(ItemAbilities.SWORD_SWEEP), "next unsuccessful drain deactivates saber");
            power.setCharge(saber, 10000); saber.use(level, player, InteractionHand.MAIN_HAND);
            var armor = (ModularArmor) stack("nano_suit_chestplate").getItem(); var chest = armor.defaultArmor();
            armor.setCharge(chest, 19000); player.setItemSlot(EquipmentSlot.CHEST, chest);
            var attacker = net.minecraft.world.entity.EntityType.COW.create(level);
            saber.getItem().hurtEnemy(saber, player, attacker);
            require(armor.charge(chest) == 250 && power.charge(saber) == 9944, "tier-two armor loses 18750 EU and saber spends 16 plus 40 EU");
            saber.getItem().hurtEnemy(saber, player, attacker);
            require(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "depleted modular armor is destroyed as in source");
            var modifiers = saber.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
            require(modifiers.modifiers().stream().noneMatch(entry -> entry.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)),
                "source saber does not add a vanilla sword attack-speed penalty");
            var web = net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
            require(saber.getDestroySpeed(web) == 15 && saber.isCorrectToolForDrops(web), "active saber cuts webs with bronze sword speed and drops");
            int beforeMining = power.charge(saber);
            saber.getItem().mineBlock(saber, level, web, player.blockPosition(), player);
            require(power.charge(saber) == beforeMining - 10, "mining nonzero-hardness block uses 10 EU");
            EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, true));
            saber.use(level, player, InteractionHand.MAIN_HAND); EquipmentControls.clear(player);
            require(saber.getDestroySpeed(web) == 1 && saber.isCorrectToolForDrops(web), "inactive saber keeps web drops but loses mining speed");
            power.setCharge(saber, 32); saber.use(level, player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < 63; tick++) saber.inventoryTick(level, player, 0, false);
            require(power.charge(saber) == 32, "unselected saber waits 64 ticks between drains");
            saber.inventoryTick(level, player, 0, false);
            require(power.charge(saber) == 16, "unselected saber spends 16 EU every 64 ticks");
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, previous); player.setItemSlot(EquipmentSlot.CHEST, oldChest); EquipmentControls.clear(player); }
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
