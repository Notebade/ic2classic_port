package example;

import ic2.api.Ic2Api;
import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class LaserClientCheck {
    private static final String[] MODES = {"mining", "low_focus", "long_range", "horizontal", "super_heat", "scatter", "explosive", "tracking"};
    private static final int[] COSTS = {1250, 100, 5000, 3000, 2500, 10000, 5000, 10000};
    private static int tick;
    private static volatile int completed;
    private static boolean sawProjectile;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.laser.verify")) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (client.player == null || server == null || client.level == null || client.isPaused()) return;
        int elapsed = ++tick - 80;
        if (elapsed < 0) return;
        int index = elapsed / 35;
        int phase = elapsed % 35;
        if (index == MODES.length) {
            require(completed == MODES.length, "all server outcomes");
            com.mojang.logging.LogUtils.getLogger().info("IC2 laser client checks passed: all eight modes");
            client.stop();
            return;
        }
        var id = client.player.getUUID();
        if (phase == 0) {
            sawProjectile = false;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var pos = player.blockPosition();
                if (index == 0) checkTap(player, pos.offset(4, 170 - pos.getY(), 0));
                player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                player.teleportTo(pos.getX() + .5, 170, pos.getZ() + .5);
                player.setYRot(0);
                player.setXRot(0);
                var level = player.serverLevel();
                level.setBlockAndUpdate(player.blockPosition().below(), Blocks.OBSIDIAN.defaultBlockState());
                var target = player.blockPosition().offset(0, 1, index == 1 ? 2 : 4);
                var laser = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:mining_laser")).orElseThrow());
                ((ConfigurableTool) laser.getItem()).setMode(laser, MODES[index]);
                ((ElectricItem) laser.getItem()).setCharge(laser, COSTS[index]);
                player.setItemInHand(InteractionHand.MAIN_HAND, laser);
                var cow = index == 0 || index == 7 ? EntityType.COW.create(level) : null;
                var block = index == 4 ? Blocks.SAND : index == 6 ? Blocks.DIRT : Blocks.STONE;
                if (cow != null) {
                    cow.setNoAi(true);
                    cow.setNoGravity(true);
                    cow.setPos(Vec3.atBottomCenterOf(target).subtract(0, .5, 0));
                    level.addFreshEntity(cow);
                } else if (index != 5) {
                    if (index == 4) level.setBlockAndUpdate(target.below(), Blocks.OBSIDIAN.defaultBlockState());
                    level.setBlockAndUpdate(target, block.defaultBlockState());
                }
                var result = index == 3 ? laser.getItem().onItemUseFirst(laser, new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(target), Direction.NORTH, target, false)))
                    : laser.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult();
                require(result.consumesAction() && ((ElectricItem) laser.getItem()).charge(laser) == 0, MODES[index] + " shot and EU");
                player.getPersistentData().putLong("laser_test_target", target.asLong());
                if (cow != null) player.getPersistentData().putUUID("laser_test_cow", cow.getUUID());
            });
        }
        for (var entity : client.level.entitiesForRendering()) {
            if (!entity.getType().builtInRegistryHolder().key().location().equals(ResourceLocation.parse("ic2:mining_laser"))) continue;
            require(client.getEntityRenderDispatcher().getRenderer(entity).getClass().getSimpleName().equals("LaserShotRenderer"), "native renderer bound");
            sawProjectile = true;
        }
        if (phase == 28) {
            require(sawProjectile, MODES[index] + " projectile reached client");
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var data = player.getPersistentData();
                var level = player.serverLevel();
                var target = BlockPos.of(data.getLong("laser_test_target"));
                if (data.hasUUID("laser_test_cow")) {
                    var mob = level.getEntity(data.getUUID("laser_test_cow"));
                    require(index == 7 ? mob == null || !mob.isAlive() : mob instanceof net.minecraft.world.entity.LivingEntity living
                        && living.getHealth() < living.getMaxHealth(), MODES[index] + " mob damage");
                    if (mob != null) mob.discard();
                    data.remove("laser_test_cow");
                } else {
                    require(index == 4 ? level.getBlockState(target).is(Blocks.GLASS) : level.isEmptyBlock(target), MODES[index] + " block outcome");
                    level.removeBlock(target, false);
                }
                completed++;
                com.mojang.logging.LogUtils.getLogger().info("IC2 laser client mode passed: {}", MODES[index]);
            });
        }
    }

    private static void checkTap(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        var log = Ic2Api.block(ResourceLocation.parse("ic2:rubber_wood")).orElseThrow();
        var collector = Ic2Api.block(ResourceLocation.parse("ic2:treetap_and_bucket")).orElseThrow();
        var logState = log.defaultBlockState();
        var collectorState = collector.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        var fill = (net.minecraft.world.level.block.state.properties.IntegerProperty) collector.getStateDefinition().getProperty("fill_stage");
        var source = pos.north();
        var oldLog = level.getBlockState(source);
        var oldCollector = level.getBlockState(pos);
        try {
            for (String name : new String[]{"resin", "collectable", "facing"}) {
                var property = log.getStateDefinition().getProperty(name);
                if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag) {
                    logState = logState.setValue(flag, true);
                } else if (property instanceof net.minecraft.world.level.block.state.properties.DirectionProperty direction) {
                    logState = logState.setValue(direction, Direction.SOUTH);
                }
            }
            level.setBlockAndUpdate(source, logState);
            level.setBlockAndUpdate(pos, collectorState);
            collectorState.tick(level, pos, level.random);
            var filled = level.getBlockState(pos);
            require(filled.is(collector) && filled.getValue(fill) > 0, "tree tap and bucket collects resin");
            filled.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
            require(level.getBlockState(pos).getValue(fill) == 0, "tree tap and bucket empties");
            require(level.getRecipeManager().byKey(ResourceLocation.parse("ic2:advanced_tree_tap")).isPresent(), "advanced tree tap recipe loads");
            com.mojang.logging.LogUtils.getLogger().info("IC2 tree tap client world checks passed");
        } finally {
            level.setBlockAndUpdate(pos, oldCollector);
            level.setBlockAndUpdate(source, oldLog);
        }
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
