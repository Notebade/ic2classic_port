package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class CableFoamChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.cable.foam.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused cable foam checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var origin = new BlockPos(1600, 120, 1600); var oldPosition = player.position();
        var oldHand = player.getMainHandItem(); var oldChest = player.getItemBySlot(EquipmentSlot.CHEST);
        var mode = player.gameMode.getGameModeForPlayer(); boolean sneak = player.isShiftKeyDown();
        var saved = new java.util.LinkedHashMap<BlockPos, BlockState>();
        for (var pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 3, 2))) saved.put(pos.immutable(), level.getBlockState(pos));
        var cable = Ic2Api.block(ResourceLocation.parse("ic2:copper_cable")).orElseThrow();
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setShiftKeyDown(false);
            player.setPos(1600.5, 120, 1597.5); player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); EquipmentControls.clear(player);
            for (var pos : saved.keySet()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(origin.below(), Ic2Api.block(ResourceLocation.parse("ic2:batbox")).orElseThrow().defaultBlockState());
            level.setBlockAndUpdate(origin, cable.defaultBlockState()); level.setBlockAndUpdate(origin.above(), cable.defaultBlockState());
            var sprayer = stack("electric_sprayer"); var power = (ElectricItem) sprayer.getItem(); var foam = (FoamContainer) sprayer.getItem();
            power.setCharge(sprayer, 100); foam.setFoam(sprayer, 100); ((ConfigurableTool) sprayer.getItem()).setMode(sprayer, "2");
            player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
            var hit = new BlockHitResult(Vec3.atCenterOf(origin), Direction.NORTH, origin, false);
            player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
            require(level.getBlockState(origin).is(cable) && foamed(level.getBlockState(origin), 1)
                && foamed(level.getBlockState(origin.above()), 1), "spraying connected cables covers both without replacing conductors");
            require(power.charge(sprayer) == 90 && foam.storedFoam(sprayer) == 98, "cable covering consumes one foam unit and 5 EU per cable");
            require(ic2.api.energy.EuNetwork.send(level, origin.above(2), 32, java.util.Set.of(Direction.DOWN), 0) > 0, "wet covered cables still conduct energy");
            var sand = new ItemStack(net.minecraft.world.item.Items.SAND, 2); player.setItemInHand(InteractionHand.MAIN_HAND, sand);
            player.gameMode.useItemOn(player, level, sand, InteractionHand.MAIN_HAND, hit);
            require(foamed(level.getBlockState(origin), 2) && sand.getCount() == 1, "sand hardens cable coating without removing the wire");
            var source = origin.west(); level.setBlockAndUpdate(source, Blocks.GOLD_BLOCK.defaultBlockState());
            var obscurator = stack("obscurator"); ((ElectricItem) obscurator.getItem()).setCharge(obscurator, 100000);
            player.setItemInHand(InteractionHand.MAIN_HAND, obscurator);
            require(ic2.core.equipment.ObscuratorItem.capture(player, InteractionHand.MAIN_HAND, source, Direction.NORTH, java.util.List.of(-1)), "copy source texture");
            player.gameMode.useItemOn(player, level, obscurator, InteractionHand.MAIN_HAND, hit);
            var coated = (ic2.core.equipment.FoamTextureEntity) level.getBlockEntity(origin);
            var texture = coated.texture(Direction.NORTH);
            require(texture != null && texture.state().is(Blocks.GOLD_BLOCK), "obscurator applies to hardened cable coating");
            var restored = (ic2.core.equipment.FoamTextureEntity) BlockEntity.loadStatic(origin, level.getBlockState(origin),
                coated.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            require(texture.equals(restored.texture(Direction.NORTH)), "covered cable surface survives serialization");
            require(ic2.api.energy.EuNetwork.send(level, origin.above(2), 32, java.util.Set.of(Direction.DOWN), 0) > 0, "camouflaged cables still conduct energy");
            checkComparing(level, player, origin, sprayer);
            checkEvents(level, player, origin, sprayer);
        } finally {
            saved.forEach(level::setBlockAndUpdate); player.setPos(oldPosition); player.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            player.setItemSlot(EquipmentSlot.CHEST, oldChest); player.setGameMode(mode); player.setShiftKeyDown(sneak); EquipmentControls.clear(player);
        }
    }
    private static void checkComparing(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, ItemStack sprayer) {
        var comparing = Ic2Api.block(ResourceLocation.parse("ic2:comparing_cable")).orElseThrow();
        level.setBlockAndUpdate(pos, comparing.defaultBlockState());
        var entity = level.getBlockEntity(pos); var data = entity.saveWithFullMetadata(level.registryAccess());
        data.putInt("min_value", 17); data.putInt("max_value", 60); entity.loadWithComponents(data, level.registryAccess());
        player.setItemInHand(InteractionHand.MAIN_HAND, sprayer); ((ConfigurableTool) sprayer.getItem()).setMode(sprayer, "1");
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
        var stored = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        require(foamed(level.getBlockState(pos), 1) && stored.getInt("min_value") == 17 && stored.getInt("max_value") == 60,
            "coating a comparing cable retains its threshold settings and entity type");
        var restored = BlockEntity.loadStatic(pos, level.getBlockState(pos), stored, level.registryAccess());
        require(restored != null && restored.getType() == entity.getType(), "comparing cable still loads its own block entity type");
    }
    private static void checkEvents(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player, BlockPos pos, ItemStack sprayer) {
        var cable = Ic2Api.block(ResourceLocation.parse("ic2:copper_cable")).orElseThrow();
        var power = (ElectricItem) sprayer.getItem(); var foam = (FoamContainer) sprayer.getItem();
        level.setBlockAndUpdate(pos, cable.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
        int charge = power.charge(sprayer), stored = foam.storedFoam(sprayer);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        java.util.function.Consumer<ic2.api.event.FoamEvent.Check> cancel = event -> { if (event.getPos().equals(pos)) event.setCanceled(true); };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
            require(foamed(level.getBlockState(pos), 0) && power.charge(sprayer) == charge && foam.storedFoam(sprayer) == stored,
                "cancelled foam selection leaves conductor and supplies unchanged");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> protect = event -> {
            if (event.getPos().equals(pos)) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protect);
        try {
            player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
            require(foamed(level.getBlockState(pos), 0) && power.charge(sprayer) == charge,
                "block protection rolls back cable coating without spending EU");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protect); }
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState()); level.setBlockAndUpdate(pos.north(), Blocks.FIRE.defaultBlockState());
        java.util.function.Consumer<ic2.api.event.FoamEvent.Check> custom = event -> {
            if (event.getState().is(Blocks.FIRE)) event.setCustomTarget(ic2.api.event.FoamEvent.TargetType.SCAFFOLD);
        };
        int[] placed = {0};
        java.util.function.Consumer<ic2.api.event.FoamEvent.Place> place = event -> {
            if (event.getState().is(Blocks.FIRE)) { placed[0]++; event.setCanceled(true); event.requestFoamPlacement(); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(custom); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(place);
        try {
            ((ConfigurableTool) sprayer.getItem()).setMode(sprayer, "4");
            player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
            require(placed[0] == 2 && power.charge(sprayer) == charge - 10 && foam.storedFoam(sprayer) == stored - 2,
                "custom scaffold targets propagate in all directions, stay in their target type and can request normal foam placement");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(custom); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(place); }
    }
    private static boolean foamed(BlockState state, int amount) { return state.getValues().entrySet().stream().anyMatch(entry -> entry.getKey().getName().equals("foamed") && entry.getValue().equals(amount)); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
