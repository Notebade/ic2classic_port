package ic2.core.machine;

import ic2.api.item.MiningDrill;
import ic2.api.item.WorldScanner;
import ic2.core.item.RechargeableItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredRegister;

final class MiningTools {
    static void register(DeferredRegister.Items items) {
        items.registerItem("drill", Drill::new);
        items.registerItem("od_scanner", p -> new Scanner(p, 50, 2, 50, 32, false));
        items.registerItem("ov_scanner", p -> new Scanner(p, 250, 4, 100, 128, false));
        items.registerItem("ov_scanner_ranged", p -> new Scanner(p, 750, 8, 100, 128, false));
        items.registerItem("fluid_scanner", p -> new Scanner(p, 100, 20, 50, 32, true));
    }
    private static int enchantment(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).entrySet().stream()
            .filter(entry -> entry.getKey().is(key)).mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue).findFirst().orElse(0);
    }
    private static final class Drill extends RechargeableItem implements MiningDrill, ic2.api.item.ElectricEnchantable {
        Drill(Properties properties) {
            super(properties.stacksTo(1).component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_IRON_TOOL),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 8), Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, 8)), 1, 0)), 10000, 100, 32);
        }
        @Override public boolean canProvideEnergy() { return false; }
        @Override public boolean acceptsElectricEnchantment(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
            return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.UNBREAKING);
        }
        @Override public boolean canUse(ItemStack stack) { return charge(stack) >= 50; }
        @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return canUse(stack) && super.isCorrectToolForDrops(stack, state); }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return canUse(stack) ? super.getDestroySpeed(stack, state) : 1; }
        @Override public boolean canMine(ItemStack stack, BlockState state, LevelReader level, BlockPos pos) {
            return canUse(stack) && state.getDestroySpeed(level, pos) >= 0 && (!state.requiresCorrectToolForDrops() || isCorrectToolForDrops(stack, state));
        }
        @Override public void consumeUse(ItemStack stack) { setCharge(stack, charge(stack) - 50); }
        @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
            if (!level.isClientSide) consumeUse(stack);
            return true;
        }
        @Override public int progressPerTick(ItemStack stack, BlockState state) { int efficiency = enchantment(stack, Enchantments.EFFICIENCY); return 1 + (efficiency == 0 ? 0 : efficiency * efficiency + 1); }
        @Override public int machineEuPerTick(ItemStack stack) { int unbreaking = enchantment(stack, Enchantments.UNBREAKING); return Math.max(1, progressPerTick(stack, null) - 2 * unbreaking * unbreaking); }
    }
    private static final class Scanner extends RechargeableItem implements WorldScanner {
        private final int cost, radius;
        private final boolean fluids;
        Scanner(Properties properties, int cost, int radius, int transfer, int packet, boolean fluids) {
            super(properties.stacksTo(1), 10000, transfer, packet); this.cost = cost; this.radius = radius; this.fluids = fluids;
        }
        @Override public boolean canProvideEnergy() { return false; }
        @Override public boolean scansFluids() { return fluids; }
        @Override public int scanRadius(ItemStack stack, boolean useEnergy) {
            if (!useEnergy) return radius;
            if (charge(stack) < cost) return 0;
            setCharge(stack, charge(stack) - cost); return radius;
        }
        @Override public boolean isValuable(ItemStack stack, BlockState state, LevelReader level, BlockPos pos) {
            return fluids ? !state.getFluidState().isEmpty() && state.getFluidState().isSource() : state.is(Tags.Blocks.ORES);
        }
        @Override public InteractionResult useOn(UseOnContext context) {
            if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
            int distance = scanRadius(context.getItemInHand(), true);
            if (distance == 0) return InteractionResult.FAIL;
            int found = 0;
            var level = context.getLevel(); var center = context.getClickedPos();
            for (var pos : BlockPos.betweenClosed(center.offset(-distance, -distance, -distance), center.offset(distance, distance, distance)))
                if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight() && level.hasChunkAt(pos)
                    && isValuable(context.getItemInHand(), level.getBlockState(pos), level, pos)) found++;
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatableWithFallback("message.ic2.scanner.result", "Scanner targets: %s", found), true);
            return InteractionResult.SUCCESS;
        }
    }
}
