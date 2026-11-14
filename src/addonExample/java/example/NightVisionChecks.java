package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;

final class NightVisionChecks {
    static void run(ServerLevel level) {
        var goggles = stack("nightvision_goggles"); var power = (ElectricItem) goggles.getItem(); var mode = (ConfigurableTool) goggles.getItem();
        require(power.capacity(goggles) == 20000 && power.transferRate(goggles) == 200 && power.minimumPacket(goggles) == 32 && !power.canProvideEnergy(goggles), "source goggles electrical profile");
        require(!goggles.isDamageableItem() && mode.mode(goggles).equals("off"), "source goggles start disabled without physical durability");
        require(!mode.setMode(goggles, "bogus") && mode.mode(goggles).equals("off"), "unknown goggles mode rejected");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        try {
            player.setItemSlot(EquipmentSlot.HEAD, goggles); power.setCharge(goggles, 10);
            tick(player); require(power.charge(goggles) == 10 && !player.hasEffect(MobEffects.NIGHT_VISION), "disabled goggles do not spend EU or grant vision");
            mode.setMode(goggles, "on"); player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200)); tick(player);
            require(power.charge(goggles) == 9 && !player.hasEffect(MobEffects.BLINDNESS) && player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 250, "enabled goggles spend one EU, remove blindness and grant source 250-tick night vision");
            mode.setMode(goggles, "off"); tick(player); require(player.hasEffect(MobEffects.NIGHT_VISION) && power.charge(goggles) == 9, "disabling preserves the naturally expiring source effect");
            player.removeEffect(MobEffects.NIGHT_VISION); power.setCharge(goggles, 0); mode.setMode(goggles, "on"); tick(player);
            require(!player.hasEffect(MobEffects.NIGHT_VISION), "empty goggles do not grant vision");
            var armor = stack("nano_suit_helmet"); var item = (ModularArmor) armor.getItem(); armor = item.defaultArmor();
            mode.setMode(goggles, "on"); power.setCharge(goggles, 25);
            require(item.install(armor, ArmorModule.GENERIC, goggles), "goggles are a real generic helmet module");
            player.setItemSlot(EquipmentSlot.HEAD, armor); item.setCharge(armor, 10); tick(player);
            require(item.charge(armor) == 9 && power.charge(item.modules(armor).getLast().stack()) == 25 && player.hasEffect(MobEffects.NIGHT_VISION), "installed goggles draw parent EU while retaining independent module battery custody");
            var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:nightvision_goggles")).orElseThrow(() -> new AssertionError("original goggles recipe exists"));
            require(recipe.value().getResultItem(level.registryAccess()).is(stack("nightvision_goggles").getItem()), "source goggles acquisition result");
            checkInput(level, player);
        } finally { player.setItemSlot(EquipmentSlot.HEAD, saved); player.removeEffect(MobEffects.NIGHT_VISION); player.removeEffect(MobEffects.BLINDNESS); }
    }
    private static void checkInput(ServerLevel level, net.minecraft.world.entity.player.Player player) {
        long now = level.getGameTime(); var oldMenu = player.containerMenu;
        try {
            for (int mask = 0; mask <= EquipmentInput.KNOWN_MASK; mask++) require(EquipmentInput.fromMask(mask).mask() == mask, "input mask round trip");
            boolean rejected = false; try { EquipmentInput.fromMask(EquipmentInput.KNOWN_MASK + 1); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "unknown input bits rejected");
            var input = new EquipmentInput(false, false, false, true, true, false);
            require(EquipmentControls.update(player, input) && EquipmentControls.current(player).equals(input), "server equipment input belongs to its player");
            ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(now + 21); require(EquipmentControls.current(player).equals(EquipmentInput.NONE), "stale held input expires without refreshing"); ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(now);
            EquipmentControls.update(player, input); player.containerMenu = player.inventoryMenu;
            var goggles = stack("nightvision_goggles"); var power = (ElectricItem) goggles.getItem(); var mode = (ConfigurableTool) goggles.getItem(); power.setCharge(goggles, 10);
            player.setItemSlot(EquipmentSlot.HEAD, goggles); tick(player);
            require(mode.mode(goggles).equals("on") && power.charge(goggles) == 9, "server HUD+Mode input toggles worn goggles"); tick(player);
            require(mode.mode(goggles).equals("on") && power.charge(goggles) == 8, "source toggle cooldown prevents repeated flips each tick");
        } finally { ((net.minecraft.world.level.storage.ServerLevelData) level.getLevelData()).setGameTime(now); player.containerMenu = oldMenu; EquipmentControls.clear(player); }
    }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow(() -> new AssertionError("source goggles exist: " + id))); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
