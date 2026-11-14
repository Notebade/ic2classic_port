package example;

import ic2.api.Ic2Api;
import net.minecraft.resources.ResourceLocation;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class ObscuratorChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.obscurator.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused obscurator checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        var tool = new net.minecraft.world.item.ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:obscurator"))
            .orElseThrow(() -> new AssertionError("source obscurator must be registered as a functional tool")));
        var power = (ic2.api.item.ElectricItem) tool.getItem(); var modes = (ic2.api.item.ConfigurableTool) tool.getItem();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getMainHandItem(); var position = player.position(); var oldMode = player.gameMode.getGameModeForPlayer();
        boolean sneaking = player.isShiftKeyDown(); var origin = new net.minecraft.core.BlockPos(1500, 110, 1500); var source = origin.north(3);
        var states = new java.util.HashMap<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var foamBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ic2:construction_foam"));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND; var up = net.minecraft.core.Direction.UP;
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setPos(1500.5, 111, 1499.5);
            player.setShiftKeyDown(false); player.setItemInHand(hand, tool); ic2.api.item.EquipmentControls.clear(player);
            states.put(source, level.getBlockState(source)); level.setBlockAndUpdate(source, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
            var colors = java.util.List.of(-1, 0x88AA55);
            require(!ic2.core.equipment.ObscuratorItem.capture(player, hand, source, up, colors), "empty obscurator cannot copy a face");
            power.setCharge(tool, 100000);
            require(ic2.core.equipment.ObscuratorItem.capture(player, hand, source, up, colors) && power.charge(tool) == 90000, "first copied surface costs 10000 EU");
            require(ic2.core.equipment.ObscuratorItem.capture(player, hand, source, up, colors) && power.charge(tool) == 90000, "copying the same surface preserves energy");
            level.setBlockAndUpdate(source, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
            require(ic2.core.equipment.ObscuratorItem.capture(player, hand, source, up, colors) && power.charge(tool) == 90000, "original replacement copy keeps its existing paid sample");
            ic2.api.item.EquipmentControls.update(player, new ic2.api.item.EquipmentInput(false, false, false, false, true, false));
            player.setShiftKeyDown(true); tool.use(level, player, hand); player.setShiftKeyDown(false); tool.use(level, player, hand);
            var sample = ic2.core.equipment.ObscuratorItem.sample(tool);
            require(sample.rotations().equals(java.util.List.of(0, 1)), "mode+sneak selects next layer; mode rotates only that layer");
            ic2.api.item.EquipmentControls.clear(player); require(modes.setMode(tool, "3"), "three-block painting mode available");
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
                states.put(pos.immutable(), level.getBlockState(pos)); level.setBlockAndUpdate(pos, foamBlock.defaultBlockState());
            }
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(origin), up, origin, false);
            var context = new net.minecraft.world.item.context.UseOnContext(player, hand, hit);
            require(tool.getItem().onItemUseFirst(tool, context).consumesAction() && power.charge(tool) == 76500, "three-by-three surface painting uses 1500 EU per changed face");
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
                var foam = (ic2.core.equipment.FoamTextureEntity) level.getBlockEntity(pos);
                require(sample.equals(foam.texture(up)) && foam.texture(net.minecraft.core.Direction.DOWN) == null, "only clicked face receives all selected layers and rotations");
            }
            require(!tool.getItem().onItemUseFirst(tool, context).consumesAction() && power.charge(tool) == 76500, "identical repaint does not consume energy");
            modes.setMode(tool, "1"); ic2.api.item.EquipmentControls.update(player, new ic2.api.item.EquipmentInput(false, false, false, false, false, true));
            tool.getItem().onItemUseFirst(tool, context);
            require(power.charge(tool) == 69000, "Alt paints the other five faces and charges only those faces");
            var foam = (ic2.core.equipment.FoamTextureEntity) level.getBlockEntity(origin);
            for (var side : net.minecraft.core.Direction.values()) require(sample.equals(foam.texture(side)), "all six faces painted");
            var restored = (ic2.core.equipment.FoamTextureEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(origin,
                foamBlock.defaultBlockState(), foam.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            for (var side : net.minecraft.core.Direction.values()) require(sample.equals(restored.texture(side)), "surface layers survive block entity persistence");
            ic2.api.item.EquipmentControls.clear(player);
            var fresh = origin.east(4); states.put(fresh, level.getBlockState(fresh)); level.setBlockAndUpdate(fresh, foamBlock.defaultBlockState());
            var freshContext = new net.minecraft.world.item.context.UseOnContext(player, hand,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(fresh), up, fresh, false));
            java.util.function.Consumer<ic2.api.event.RetextureEvent> protect = event -> event.setCanceled(true);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protect);
            try { require(!tool.getItem().onItemUseFirst(tool, freshContext).consumesAction() && power.charge(tool) == 69000, "canceled texture change costs no energy"); }
            finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protect); }
            power.setCharge(tool, 1499);
            require(!tool.getItem().onItemUseFirst(tool, freshContext).consumesAction()
                && ((ic2.core.equipment.FoamTextureEntity) level.getBlockEntity(fresh)).texture(up) == null, "insufficient charge preserves the surface");
        } finally {
            states.forEach(level::setBlockAndUpdate); player.setItemInHand(hand, saved); player.setPos(position);
            player.setGameMode(oldMode); player.setShiftKeyDown(sneaking); ic2.api.item.EquipmentControls.clear(player);
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
