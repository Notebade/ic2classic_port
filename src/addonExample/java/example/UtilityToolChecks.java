package example;

import ic2.api.Ic2Api;
import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class UtilityToolChecks {
    static void run(ServerLevel level) {
        NanoSaberChecks.run(level);
        FoodCanEffectsChecks.run(level);
        ObscuratorChecks.run(level);
        PrecisionWrenchChecks.run(level);
        FoamChecks.run(level);
        checkChainsaws(level);
        for (boolean advanced : new boolean[]{false, true}) {
            String id = advanced ? "advanced_hoe" : "electric_hoe";
            var hoe = new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source electric hoe exists: " + id)));
            var power = (ElectricItem) hoe.getItem();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var saved = player.getMainHandItem().copy();
            var center = new BlockPos(470, 100, advanced ? 470 : 480);
            try {
                player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
                for (var pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2 | 16);
                    level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false));
                hoe.useOn(context);
                require(level.getBlockState(center).is(Blocks.DIRT), "empty hoe cannot till");
                power.setCharge(hoe, 1000);
                if (advanced) require(((ConfigurableTool) hoe.getItem()).setMode(hoe, "1"), "advanced hoe radius configurable");
                require(hoe.useOn(context).consumesAction(), "charged hoe tills");
                int tilled = 0;
                for (var pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) if (level.getBlockState(pos).is(Blocks.FARMLAND)) tilled++;
                require(tilled == (advanced ? 9 : 1) && power.charge(hoe) == (advanced ? 100 : 950), "source radius and per-block EU cost");
                require(power.capacity() == (advanced ? 100000 : 10000) && power.minimumPacket() == (advanced ? 512 : 32), "source hoe charging contract");
            } finally { player.setItemInHand(InteractionHand.MAIN_HAND, saved); }
        }
    }
    private static void checkChainsaws(ServerLevel level) {
        for (String id : java.util.List.of("chainsaw", "advanced_chainsaw")) {
            var saw = new ItemStack(Ic2Api.item(ResourceLocation.fromNamespaceAndPath("ic2", id)).orElseThrow(() -> new AssertionError("source chainsaw exists: " + id)));
            var power = (ElectricItem) saw.getItem();
            boolean advanced = id.startsWith("advanced");
            require(saw.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 1, "empty saw loses powered speed");
            power.setCharge(saw, 1000);
            require(saw.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 12 && power.minimumPacket() == (advanced ? 512 : 32), "source saw wood speed and charge tier");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
            require(saw.getItem().interactLivingEntity(saw, player, sheep, InteractionHand.MAIN_HAND).consumesAction() && sheep.isSheared(), "chainsaw actually shears a sheep");
            require(power.charge(saw) == (advanced ? 800 : 900), "source shearing consumes two uses");
            int beforePlant = power.charge(saw);
            saw.getItem().mineBlock(saw, level, Blocks.DANDELION.defaultBlockState(), BlockPos.ZERO, player);
            require(power.charge(saw) == beforePlant, "zero-hardness block consumes no saw energy");
            var pos = new BlockPos(490, 100, 490);
            var drops = net.minecraft.world.level.block.Block.getDrops(Blocks.OAK_LEAVES.defaultBlockState(), level, pos, null, player, saw);
            require(drops.size() == 1 && drops.getFirst().is(Blocks.OAK_LEAVES.asItem()), "chainsaw harvests intact leaves");
            var target = net.minecraft.world.entity.EntityType.SHEEP.create(level);
            saw.getItem().hurtEnemy(saw, target, player);
            require(target.getHealth() == Math.max(0, target.getMaxHealth() - 10) && power.charge(saw) == beforePlant - (advanced ? 100 : 50), "chainsaw source powered attack and EU drain");
            if (advanced) checkTree(level, player, saw, power);
        }
    }
    private static void checkTree(ServerLevel level, net.minecraft.server.level.ServerPlayer player, ItemStack saw, ElectricItem power) {
        var saved = player.getMainHandItem().copy(); var oldMode = player.gameMode.getGameModeForPlayer();
        var base = new BlockPos(495, 100, 495);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, saw);
            player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            for (int y = 0; y < 3; y++) level.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 2 | 16);
            level.setBlock(base.above(3), Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 1), 2 | 16);
            power.setCharge(saw, 1000);
            require(player.gameMode.destroyBlock(base), "advanced chainsaw chops selected tree");
            for (int y = 0; y < 3; y++) require(level.isEmptyBlock(base.above(y)), "advanced chainsaw fells connected logs");
            require(level.getBlockState(base.above(3)).is(Blocks.OAK_LEAVES) && power.charge(saw) == 700, "tree felling keeps crown and charges per removed log");
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, saved); player.gameMode.changeGameModeForPlayer(oldMode); }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
