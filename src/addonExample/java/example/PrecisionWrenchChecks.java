package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import ic2.api.item.PrecisionWrench;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class PrecisionWrenchChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.wrench.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused precision wrench checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        if (PrecisionWrench.dismantling(Blocks.DIAMOND_BLOCK) == null) PrecisionWrench.registerDismantling(Blocks.DIAMOND_BLOCK,
            new PrecisionWrench.Dismantling(0, context -> java.util.List.of(new ItemStack(Blocks.GOLD_BLOCK))));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getMainHandItem(); var pos = new BlockPos(474, 110, 474); var original = level.getBlockState(pos);
        var wrench = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:precision_wrench")).orElseThrow());
        var power = (ElectricItem) wrench.getItem(); var drops = new java.util.ArrayList<ItemStack>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                    && item.blockPosition().distSqr(pos) < 4) { drops.add(item.getItem().copy()); event.setCanceled(true); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()); power.setCharge(wrench, 9999);
            require(!wrench.getItem().onItemUseFirst(wrench, context).consumesAction() && level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK),
                "loss protection requires the original 100-use energy reserve");
            for (int use = 0; use < 30; use++) {
                level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()); power.setCharge(wrench, 40000); drops.clear();
                require(wrench.getItem().onItemUseFirst(wrench, context).consumesAction() && level.isEmptyBlock(pos), "protected wrench dismantles registered block");
                require(drops.size() == 1 && drops.getFirst().is(Blocks.GOLD_BLOCK.asItem()) && power.charge(wrench) == 19000,
                    "prevented loss returns intact drop and costs 1000 plus 20000 EU");
            }
            level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()); power.setCharge(wrench, 40000); drops.clear();
            wrench.getItem().onItemUseFirst(wrench, context);
            require(drops.size() == 1 && drops.getFirst().is(Blocks.DIAMOND_BLOCK.asItem()) && power.charge(wrench) == 39000,
                "after thirty prevented losses normal loot and ordinary energy cost return");
            CustomData.update(DataComponents.CUSTOM_DATA, wrench, tag -> tag.putBoolean("lossless", true));
            level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState()); power.setCharge(wrench, 40000); drops.clear();
            wrench.getItem().onItemUseFirst(wrench, context);
            require(drops.size() == 1 && drops.getFirst().is(Blocks.GOLD_BLOCK.asItem()) && power.charge(wrench) == 19000,
                "infinite upgrade continues preventing losses after the finite budget is exhausted");
            level.setBlockAndUpdate(pos, Blocks.OAK_STAIRS.defaultBlockState()); power.setCharge(wrench, 40000);
            require(wrench.getItem().onItemUseFirst(wrench, context).consumesAction()
                && level.getBlockState(pos).getValue(net.minecraft.world.level.block.StairBlock.HALF) == net.minecraft.world.level.block.state.properties.Half.TOP
                && power.charge(wrench) == 39900, "upward wrench face flips stairs upside down for one use");
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
                net.minecraft.world.level.block.state.properties.ChestType.LEFT), 2); power.setCharge(wrench, 40000);
            var east = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false));
            require(!wrench.getItem().onItemUseFirst(wrench, east).consumesAction() && power.charge(wrench) == 40000, "double chest cannot rotate");
            var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("ic2:precision_wrench_lossless")).orElseThrow().value();
            var inputs = new java.util.ArrayList<ItemStack>();
            for (String id : java.util.List.of("upgrade_efficiency", "obscurator", "upgrade_efficiency", "plasma_core", "precision_wrench", "plasma_core",
                    "electric_vent_advanced", "advanced_circuit", "electric_vent_advanced")) {
                var ingredient = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow());
                ingredient.setCount(id.equals("upgrade_efficiency") || id.equals("advanced_circuit") ? 10 : id.equals("plasma_core") ? 5 : 1);
                if (id.equals("precision_wrench")) ((ElectricItem) ingredient.getItem()).setCharge(ingredient, 20000);
                if (id.equals("obscurator")) ((ElectricItem) ingredient.getItem()).setCharge(ingredient, 5000);
                inputs.add(ingredient);
            }
            @SuppressWarnings("unchecked") var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) recipe;
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
            require(crafting.matches(grid, level), "infinite wrench recipe accepts original ingredient counts");
            var upgraded = crafting.assemble(grid, level.registryAccess());
            require(upgraded.get(DataComponents.CUSTOM_DATA).copyTag().getBoolean("lossless") && power.charge(upgraded) == 25000,
                "infinite recipe marks lossless state and carries ingredient energy");
            inputs.getFirst().shrink(1);
            require(!crafting.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs), level), "recipe rejects nine efficiency upgrades where ten are required");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            level.setBlockAndUpdate(pos, original); player.setItemInHand(InteractionHand.MAIN_HAND, saved);
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
