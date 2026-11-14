package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class JetpackChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.jetpack.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused jetpack checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var position = player.position(); var motion = player.getDeltaMovement(); boolean ground = player.onGround(), sneak = player.isShiftKeyDown();
        try {
            player.setPos(750, 100, 750); player.setOnGround(false); player.setShiftKeyDown(false);
            var pack = charged("electric_jetpack"); var power = (ElectricItem) pack.getItem();
            var mode = (ConfigurableTool) pack.getItem(); mode.setMode(pack, "hover");
            EquipmentControls.clear(player);
            for (int tick = 0; tick < 5; tick++) {
                player.setDeltaMovement(0, -.3, 0); tick(pack, player);
                require(Math.abs(player.getDeltaMovement().y + .2) < .0001, "basic hover clamps falling speed");
            }
            require(power.charge(pack) == 29996, "basic hover spends 4 EU once per five ticks");
            player.setDeltaMovement(0, -.3, 0); tick(pack, player);
            require(power.charge(pack) == 29992, "basic hover resumes fuel charge on sixth tick");
            checkControls(player);
            checkRocket(player, "compacted_electric_jetpack", 30, 1.4);
            checkRocket(player, "nuclear_jetpack", 12, .95);
            checkRocket(player, "compacted_nuclear_jetpack", 40, 1.8);
            checkFuel(player);
            checkModule(player);
            checkNuclear(player);
            checkLimits(player);
            checkRecipes(level);
            require(((ElectricItem) charged("compacted_nuclear_jetpack").getItem()).minimumPacket() == 32,
                "compact nuclear jetpack retains original tier one input");
        } finally {
            player.setPos(position); player.setDeltaMovement(motion); player.setOnGround(ground); player.setShiftKeyDown(sneak);
            EquipmentControls.clear(player);
        }
    }
    private static void checkControls(net.minecraft.world.entity.player.Player player) {
        var pack = charged("compacted_electric_jetpack"); var mode = (ConfigurableTool) pack.getItem();
        var hoverKeys = new EquipmentInput(true, false, false, false, true, false);
        EquipmentControls.update(player, hoverKeys); tick(pack, player);
        require(mode.mode(pack).equals("hover"), "M+Space enables basic hover");
        for (int i = 0; i < 10; i++) tick(pack, player);
        require(mode.mode(pack).equals("advanced_hover"), "held M+Space advances hover after ten ticks");
        for (int i = 0; i < 10; i++) tick(pack, player);
        require(mode.mode(pack).equals("normal"), "third hover setting returns to normal");
        EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false, false, true));
        player.setDeltaMovement(Vec3.ZERO); int fuel = ((ElectricItem) pack.getItem()).charge(pack); tick(pack, player);
        require(player.getDeltaMovement().equals(Vec3.ZERO) && ((ElectricItem) pack.getItem()).charge(pack) == fuel, "V disables jetpack thrust");
        for (int i = 0; i < 11; i++) tick(pack, player);
        EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false)); tick(pack, player);
        require(player.getDeltaMovement().y > 0, "V re-enables after the source cooldown");
        pack = charged("electric_jetpack");
        EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, true));
        player.setDeltaMovement(0, -.3, 0); tick(pack, player);
        require(Math.abs(player.getDeltaMovement().y + .2) < .0001 && ((ConfigurableTool) pack.getItem()).mode(pack).equals("normal"),
            "Alt temporarily enables hover without changing saved mode");
    }
    private static void checkRocket(net.minecraft.world.entity.player.Player player, String id, int cost, double thrust) {
        var pack = charged(id); var power = (ElectricItem) pack.getItem(); int before = power.charge(pack);
        player.setDeltaMovement(Vec3.ZERO);
        EquipmentControls.update(player, new EquipmentInput(false, true, false, false, true, false)); tick(pack, player);
        EquipmentControls.update(player, new EquipmentInput(false, true, false, false, false, false));
        for (int i = 1; i < 10; i++) tick(pack, player);
        require(power.charge(pack) == before - cost * 50 && player.getDeltaMovement().equals(Vec3.ZERO), id + " charges rocket without ordinary thrust");
        EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, true)); tick(pack, player);
        require(Math.abs(player.getDeltaMovement().y - thrust / 4) < .0001 && power.charge(pack) == before - cost * 50 - 50,
            id + " releases source rocket impulse and spends extra 50 EU");
        require(!pack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("RocketMode"), "rocket releases only once");
    }
    private static void checkFuel(net.minecraft.world.entity.player.Player player) {
        var fuel = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.parse("ic2:bio_fuel"));
        var action = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        for (String id : java.util.List.of("fuel_jetpack", "compact_fuel_jetpack")) {
            var pack = stack(id); var tank = pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
            tank.fill(new net.neoforged.neoforge.fluids.FluidStack(fuel, Integer.MAX_VALUE), action);
            ((ConfigurableTool) pack.getItem()).setMode(pack, "hover"); EquipmentControls.clear(player);
            int before = tank.getFluidInTank(0).getAmount();
            for (int i = 0; i < 5; i++) { player.setDeltaMovement(0, -.3, 0); tick(pack, player); }
            require(tank.getFluidInTank(0).getAmount() == before - (id.equals("fuel_jetpack") ? 6 : 30), id + " hover consumes fuel once per five ticks");
        }
        var pack = stack("compact_fuel_jetpack"); var tank = pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        tank.fill(new net.neoforged.neoforge.fluids.FluidStack(fuel, 10000), action);
        player.setDeltaMovement(Vec3.ZERO);
        EquipmentControls.update(player, new EquipmentInput(false, true, false, false, true, false)); tick(pack, player);
        EquipmentControls.update(player, new EquipmentInput(false, true, false, false, false, false));
        for (int i = 1; i < 10; i++) tick(pack, player);
        EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, true)); tick(pack, player);
        require(Math.abs(player.getDeltaMovement().y - .3) < .0001 && tank.getFluidInTank(0).getAmount() == 8200,
            "compact fuel rocket uses stored fuel and extra 50 units on release");
    }
    private static void checkModule(net.minecraft.world.entity.player.Player player) {
        var item = (ModularArmor) stack("nano_suit_chestplate").getItem(); var armor = item.defaultArmor();
        item.setCharge(armor, 1000); var module = charged("electric_jetpack");
        require(item.install(armor, ArmorModule.BACK, module), "electric jetpack installs in chest back slot");
        EquipmentControls.update(player, new EquipmentInput(true, false, false, false, false, false));
        player.setDeltaMovement(Vec3.ZERO); tick(armor, player);
        require(player.getDeltaMovement().y > 0 && item.charge(armor) == 993, "installed jetpack consumes armor energy");
        var removed = item.removeModule(armor, 1, false);
        require(((ElectricItem) removed.getItem()).charge(removed) == 30000, "installed jetpack retains its separate original battery");
    }
    private static void checkNuclear(net.minecraft.world.entity.player.Player wearer) {
        // FakePlayer deliberately suppresses menu opening; retain the real provider and menu with a transport-only stub.
        var player = new net.neoforged.neoforge.common.util.FakePlayer((net.minecraft.server.level.ServerLevel) wearer.level(),
                new com.mojang.authlib.GameProfile(java.util.UUID.fromString("635e4557-cd51-4a59-a7d9-81e4778ea284"), "JetpackMenuCheck")) {
            @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider,
                    java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer) {
                containerMenu = provider.createMenu(1, getInventory(), this); return java.util.OptionalInt.of(1);
            }
        };
        player.setPos(wearer.position());
        var original = player.getMainHandItem(); var pack = charged("nuclear_jetpack");
        var power = (ElectricItem) pack.getItem();
        try {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pack);
            EquipmentControls.update(player, new EquipmentInput(false, false, false, false, false, false, true, false));
            pack.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            var menu = player.containerMenu;
            require(menu.slots.size() == 61, "nuclear menu has source 5x5 reactor grid plus player inventory");
            require(!menu.getSlot(0).mayPlace(new ItemStack(net.minecraft.world.item.Items.DIRT)) && menu.getSlot(0).mayPlace(stack("uranium_rod_single")),
                "nuclear menu accepts only reactor components");
            menu.getSlot(0).setByPlayer(stack("uranium_rod_single"));
            require(pack.get(DataComponents.CONTAINER).getStackInSlot(0).is(stack("uranium_rod_single").getItem()), "reactor menu saves components");
            player.closeContainer(); EquipmentControls.clear(player);
            tick(pack, player);
            var rod = pack.get(DataComponents.CONTAINER).getStackInSlot(0).copy();
            for (int i = 0; i < 20; i++) tick(pack, player);
            require(ItemStack.matches(rod, pack.get(DataComponents.CONTAINER).getStackInSlot(0)), "full nuclear pack does not burn fuel");
            power.setCharge(pack, 10000); for (int i = 0; i < 20; i++) tick(pack, player);
            require(power.charge(pack) == 10000, "reactor stays idle above 30 percent charge");
            power.setCharge(pack, 9000); for (int i = 0; i < 20; i++) tick(pack, player);
            require(power.charge(pack) > 9000 && !ItemStack.matches(rod, pack.get(DataComponents.CONTAINER).getStackInSlot(0)), "reactor starts at 30 percent and burns a real fuel rod");
            power.setCharge(pack, power.capacity() - 1); tick(pack, player);
            require(power.charge(pack) == power.capacity(), "reactor stops at full charge without overflowing");
            power.setCharge(pack, 15000); for (int i = 0; i < 20; i++) tick(pack, player);
            require(power.charge(pack) == 15000, "full cutoff retains hysteresis after discharging");
            var shell = (ModularArmor) stack("nano_suit_chestplate").getItem(); var armor = shell.defaultArmor();
            require(shell.install(armor, ArmorModule.BACK, pack.copy()), "nuclear pack installs as back module");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, armor);
            EquipmentControls.update(player, EquipmentInput.fromMask(64)); armor.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            require(player.containerMenu.slots.size() == 61 && player.containerMenu.getSlot(0).getItem().is(stack("uranium_rod_single").getItem()),
                "C+right click opens installed reactor without losing contents");
            player.containerMenu.getSlot(0).setByPlayer(ItemStack.EMPTY); player.closeContainer();
            require(shell.modules(armor).get(1).stack().get(DataComponents.CONTAINER).stream().allMatch(ItemStack::isEmpty), "installed reactor menu persists removal in armor module");
            EquipmentControls.clear(player);
            var hot = charged("nuclear_jetpack");
            CustomData.update(DataComponents.CUSTOM_DATA, hot, tag -> tag.putInt("reactor_heat", 10000));
            boolean[] exploded = {false};
            java.util.function.Consumer<net.neoforged.neoforge.event.level.ExplosionEvent.Start> stopBlast = event -> {
                if (event.getExplosion().getDirectSourceEntity() == player) { exploded[0] = true; event.setCanceled(true); }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(stopBlast);
            try { tick(hot, player); require(hot.isEmpty() && exploded[0], "overheated nuclear pack is consumed and starts owner-attributed explosion"); }
            finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(stopBlast); }
        } finally { player.closeContainer(); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, original); EquipmentControls.clear(player); }
    }
    private static void checkLimits(net.minecraft.world.entity.player.Player player) {
        var pack = charged("electric_jetpack"); var power = (ElectricItem) pack.getItem(); power.setCharge(pack, 2);
        player.setDeltaMovement(Vec3.ZERO); EquipmentControls.update(player, EquipmentInput.fromMask(1)); tick(pack, player);
        require(player.getDeltaMovement().y > 0 && power.charge(pack) == 0, "last partial fuel still provides reduced thrust before emptying");
        pack = charged("compacted_electric_jetpack"); power = (ElectricItem) pack.getItem();
        ((ConfigurableTool) pack.getItem()).setMode(pack, "advanced_hover");
        player.setDeltaMovement(0, -.1, 0); EquipmentControls.clear(player); tick(pack, player);
        require(Math.abs(player.getDeltaMovement().y) < .0001 && power.charge(pack) == power.capacity() - 30,
            "advanced hover holds altitude and spends normal mode energy");
    }
    private static void checkRecipes(net.minecraft.server.level.ServerLevel level) {
        var holder = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:compacted_nuclear_jetpack_1"));
        require(holder.isPresent(), "alternate compact nuclear upgrade recipe is present");
        var slots = java.util.List.of(ItemStack.EMPTY, charged("batpack"), ItemStack.EMPTY,
            stack("advanced_circuit").copyWithCount(4), charged("nuclear_jetpack"), stack("advanced_circuit").copyWithCount(4),
            charged("electric_jetpack"), charged("lappack"), charged("electric_jetpack"));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, slots);
        @SuppressWarnings("unchecked") var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) holder.get().value();
        require(recipe.matches(input, level), "alternate compact nuclear recipe accepts counted components");
        var output = recipe.assemble(input, level.registryAccess());
        long sum = slots.stream().filter(s -> s.getItem() instanceof ElectricItem).mapToLong(s -> ((ElectricItem) s.getItem()).charge(s)).sum();
        require(output.is(stack("compacted_nuclear_jetpack").getItem()) && ((ElectricItem) output.getItem()).charge(output) == Math.min(360000, sum), "alternate upgrade carries ingredient energy");
        for (String id : java.util.List.of("electric_jetpack", "nuclear_jetpack")) {
            var repair = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("ic2:" + id + "_repair")).orElseThrow().value();
            var pack = charged(id); CustomData.update(DataComponents.CUSTOM_DATA, pack, tag -> tag.putBoolean("SpecialMode", true));
            pack.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(stack("uranium_rod_single"))));
            var ingredients = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(pack, stack("magnet")));
            require(repair.matches(ingredients, level), "magnet repairs special mode jetpack");
            var repaired = repair.assemble(ingredients, level.registryAccess());
            require(!repaired.get(DataComponents.CUSTOM_DATA).copyTag().getBoolean("SpecialMode")
                && repaired.get(DataComponents.CONTAINER).equals(pack.get(DataComponents.CONTAINER))
                && ((ElectricItem) repaired.getItem()).charge(repaired) == ((ElectricItem) pack.getItem()).charge(pack), "repair preserves charge and reactor contents");
            require(!repair.matches(net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(repaired, stack("magnet"))), level), "normal pack cannot waste a repair magnet");
        }
    }
    private static void tick(ItemStack stack, net.minecraft.world.entity.player.Player player) { ((ArmorTickItem) stack.getItem()).tickArmor(stack, player, EquipmentSlot.CHEST); }
    private static ItemStack charged(String id) { var stack = stack(id); var power = (ElectricItem) stack.getItem(); power.setCharge(stack, power.capacity()); return stack; }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
