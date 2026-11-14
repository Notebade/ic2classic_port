package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class FoamChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void serverStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.foam.verify")) return;
        try {
            run(event.getServer().overworld());
            com.mojang.logging.LogUtils.getLogger().info("IC2 focused foam checks passed");
        } finally { event.getServer().halt(false); }
    }
    static void run(ServerLevel level) {
        CableFoamChecks.run(level);
        var sprayer = stack("electric_sprayer");
        require(sprayer.getItem() instanceof ConfigurableTool, "electric sprayer exposes all six original modes");
        var modes = (ConfigurableTool) sprayer.getItem();
        require(modes.modes().equals(java.util.List.of("1", "2", "4", "8", "16", "32")), "source spray mode limits");
        var power = (ElectricItem) sprayer.getItem();
        var foam = (FoamContainer) sprayer.getItem();
        var wet = Ic2Api.block(ResourceLocation.parse("ic2:wet_construction_foam")).orElseThrow();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var savedHand = player.getMainHandItem(); var savedChest = player.getItemBySlot(EquipmentSlot.CHEST);
        var position = player.position(); var oldMode = player.gameMode.getGameModeForPlayer();
        boolean crouched = player.isShiftKeyDown(); float yaw = player.getYRot(), pitch = player.getXRot();
        var origin = new BlockPos(480, 150, 480);
        var saved = new java.util.HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (var p : BlockPos.betweenClosed(origin.offset(-9, -9, -9), origin.offset(9, 9, 9))) saved.put(p.immutable(), level.getBlockState(p));
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(false); player.setYRot(0); player.setXRot(0); player.setPos(480.5, 150, 477.5);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
            var hit = new BlockHitResult(Vec3.atCenterOf(origin.below()), Direction.UP, origin.below(), false);
            var use = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
            for (String mode : modes.modes()) {
                for (var pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
                int count = Integer.parseInt(mode);
                require(modes.setMode(sprayer, mode), "select foam mode " + mode);
                foam.setFoam(sprayer, 1000); power.setCharge(sprayer, count * 5 - 1);
                require(!sprayer.useOn(use).consumesAction() && level.isEmptyBlock(origin), "full spray must be affordable before placement");
                power.setCharge(sprayer, count * 5);
                require(sprayer.useOn(use).consumesAction(), "spray mode " + mode + " places foam");
                long placed = saved.keySet().stream().filter(pos -> level.getBlockState(pos).is(wet)).count();
                require(placed == count && foam.storedFoam(sprayer) == 1000 - count && power.charge(sprayer) == 0,
                    "mode " + mode + " places its block limit for five EU and one foam per block: " + placed);
                require(saved.keySet().stream().noneMatch(pos -> pos.getZ() < origin.getZ() && level.getBlockState(pos).is(wet)),
                    "forward spray does not spread behind the player");
            }
            for (var pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 2 | 16);
            var manual = stack("cf_sprayer"); var pack = stack("cf_pack");
            var manualFoam = (FoamContainer) manual.getItem(); var packFoam = (FoamContainer) pack.getItem();
            manualFoam.setFoam(manual, 0); packFoam.setFoam(pack, 26);
            player.setItemSlot(EquipmentSlot.CHEST, pack); player.setItemInHand(InteractionHand.MAIN_HAND, manual);
            require(manual.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "empty manual sprayer draws from worn pack");
            require(packFoam.storedFoam(pack) == 13 && manualFoam.storedFoam(manual) == 0, "manual spray uses thirteen pack units");
            var refill = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:cf_refill")).orElseThrow().value();
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(manual, stack("cf_pellet")));
            require(((net.minecraft.world.item.crafting.CraftingRecipe) refill).matches(grid, level), "pellet refill recipe accepts empty sprayer");
            var filled = ((net.minecraft.world.item.crafting.CraftingRecipe) refill).assemble(grid, level.registryAccess());
            require(manualFoam.storedFoam(filled) == 100, "crafting pellet restores 100 manual foam uses");
            checkCanner(level, origin.offset(8, 0, 8));
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
            modes.setMode(sprayer, "1"); foam.setFoam(sprayer, 100); power.setCharge(sprayer, 100);
            level.setBlockAndUpdate(origin.below(), Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
            player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
            require(level.getBlockState(origin).is(wet) && power.charge(sprayer) == 95,
                "sprayer use-first places foam before interactive block opens its menu");
        } finally {
            saved.forEach((pos, state) -> level.setBlock(pos, state, 2 | 16));
            player.setItemInHand(InteractionHand.MAIN_HAND, savedHand); player.setItemSlot(EquipmentSlot.CHEST, savedChest);
            player.setPos(position); player.setYRot(yaw); player.setXRot(pitch); player.setShiftKeyDown(crouched); player.setGameMode(oldMode);
        }
    }
    private static void checkCanner(ServerLevel level, BlockPos pos) {
        for (String id : java.util.List.of("electric_sprayer", "cf_pack", "cf_sprayer")) {
            level.removeBlock(pos, false);
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:canner")).orElseThrow().defaultBlockState());
            var input = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            var output = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
            var container = stack(id); var foam = (FoamContainer) container.getItem();
            int pellets = id.equals("cf_sprayer") ? 1 : 2;
            foam.setFoam(container, foam.foamCapacity(container) - foam.refillAmount() * pellets);
            require(input.insertItem(0, container, false).isEmpty() && input.insertItem(1, stack("cf_pellet").copyWithCount(pellets), false).isEmpty(),
                "canner accepts " + id + " and foam pellets");
            tickCanner(level, pos, 51 * pellets);
            require(input.getStackInSlot(0).isEmpty() && input.getStackInSlot(1).isEmpty()
                && foam.storedFoam(output.getStackInSlot(0)) == foam.foamCapacity(container), "canner fills " + id + " with source batches and moves it to output");
            var next = stack(id); foam.setFoam(next, 0);
            input.insertItem(0, next, false); input.insertItem(1, stack("cf_pellet"), false);
            tickCanner(level, pos, 60);
            require(foam.storedFoam(input.getStackInSlot(0)) == 0 && input.getStackInSlot(1).getCount() == 1,
                "occupied output prevents further foam filling without consuming pellets");
        }
    }
    private static void tickCanner(ServerLevel level, BlockPos pos, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            ic2.api.energy.EuNetwork.send(level, pos.above(), 32, java.util.Set.of(Direction.DOWN), 0);
            ApiSmokeChecks.tick(level, pos, level.getBlockEntity(pos).getType());
        }
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
