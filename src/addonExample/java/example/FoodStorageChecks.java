package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class FoodStorageChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.food.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused food storage checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(ServerLevel level) {
        var chestArmor = (ModularArmor) stack("nano_suit_chestplate").getItem();
        var chest = chestArmor.defaultArmor();
        require(chestArmor.install(chest, ArmorModule.STORAGE, stack("food_storage_module")), "install food storage");
        require(!chestArmor.removeModule(chest, 1, false).isEmpty(), "newly installed empty food storage can be removed without touching the battery");
        require(chestArmor.modules(chest).size() == 1 && chestArmor.capacity(chest) == 100000, "battery survives empty storage removal");
        require(chestArmor.install(chest, ArmorModule.STORAGE, stack("food_storage_module")), "reinstall empty storage");
        var before = chestArmor.modules(chest);
        var contents = foodComponent();
        chest.set(contents, ItemContainerContents.fromItems(java.util.List.of(stack("filled_tin_can"), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)));
        require(sameModules(chestArmor.modules(chest), before) && chestArmor.capacity(chest) == 100000, "food contents preserve installed modules and capacity");
        require(chestArmor.removeModule(chest, 1, false).isEmpty(), "nonempty storage cannot be removed");
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var oldHead = player.getItemBySlot(EquipmentSlot.HEAD); var oldChest = player.getItemBySlot(EquipmentSlot.CHEST);
        var inventory = player.getInventory(); var saved = inventory.items.stream().map(ItemStack::copy).toList();
        int hunger = player.getFoodData().getFoodLevel(); float saturation = player.getFoodData().getSaturationLevel(), health = player.getHealth();
        var mode = player.gameMode.getGameModeForPlayer();
        try {
            inventory.items.clear(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, chest);
            EquipmentControls.update(player, EquipmentInput.fromMask(64));
            require(chest.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction()
                && player.getMainHandItem() == chest, "side-inventory key opens food storage without equipping the held armor");
            EquipmentControls.clear(player); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var armor = (ModularArmor) stack("nano_suit_helmet").getItem(); var helmet = armor.defaultArmor();
            require(armor.install(helmet, ArmorModule.GENERIC, stack("auto_feeder_module")), "install feeder in Nano helmet");
            player.setItemSlot(EquipmentSlot.HEAD, helmet); player.setItemSlot(EquipmentSlot.CHEST, chest);
            player.getFoodData().setFoodLevel(18); player.setHealth(16); armor.setCharge(helmet, 999);
            tick(player);
            require(player.getFoodData().getFoodLevel() == 18 && chest.get(contents).getStackInSlot(0).getCount() == 1, "no EU means no food consumption");
            armor.setCharge(helmet, 2000); tick(player);
            require(player.getFoodData().getFoodLevel() == 20 && player.getHealth() == 18 && armor.charge(helmet) == 1000, "stored can restores hunger and source healing for 1000 EU");
            require(chest.get(contents).getStackInSlot(0).isEmpty() && chest.get(contents).getStackInSlot(2).is(stack("tin_can").getItem()), "empty can goes into storage output");
            require(sameModules(chestArmor.modules(chest), before), "feeding does not alter installed armor modules");
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            var box = stack("carbon_box"); var toolbox = (Toolbox) box.getItem();
            require(toolbox.store(box, 0, stack("filled_tin_can")), "toolbox accepts food can");
            inventory.setItem(0, box); player.getFoodData().setFoodLevel(18); player.setHealth(16);
            tick(player);
            require(player.getFoodData().getFoodLevel() == 20 && player.getHealth() == 18 && armor.charge(helmet) == 0, "toolbox can feeds and heals with exact EU cost");
            require(toolbox.stored(box, 0).is(stack("tin_can").getItem()) || inventory.items.stream().anyMatch(item -> item.is(stack("tin_can").getItem())), "toolbox feeding preserves the empty can");
        } finally {
            for (int slot = 0; slot < saved.size(); slot++) inventory.setItem(slot, saved.get(slot));
            player.setItemSlot(EquipmentSlot.HEAD, oldHead); player.setItemSlot(EquipmentSlot.CHEST, oldChest);
            player.getFoodData().setFoodLevel(hunger); player.getFoodData().setSaturation(saturation); player.setHealth(health); player.setGameMode(mode);
        }
    }
    @SuppressWarnings("unchecked") static DataComponentType<ItemContainerContents> foodComponent() {
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("ic2:armor_food"));
        if (type == null) throw new AssertionError("Food has its own persistent armor component");
        return (DataComponentType<ItemContainerContents>) type;
    }
    private static boolean sameModules(java.util.List<InstalledArmorModule> actual, java.util.List<InstalledArmorModule> expected) {
        if (actual.size() != expected.size()) return false;
        for (int index = 0; index < actual.size(); index++)
            if (!actual.get(index).category().equals(expected.get(index).category()) || !ItemStack.matches(actual.get(index).stack(), expected.get(index).stack())) return false;
        return true;
    }
    private static void tick(net.minecraft.world.entity.player.Player player) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player)); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
