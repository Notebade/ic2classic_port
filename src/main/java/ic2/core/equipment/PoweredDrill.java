package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricEnchantable;
import ic2.api.item.MiningDrill;
import ic2.core.item.RechargeableItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.registries.DeferredRegister;

final class PoweredDrill extends RechargeableItem implements MiningDrill, ElectricEnchantable, ConfigurableTool {
    private static final TagKey<Block> ROCK = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("ic2", "rock_cutter"));
    private final Kind kind;
    private final Set<UUID> miningArea = new HashSet<>();
    private enum Kind {
        DIAMOND("diamond_drill", 80, 16, 3, 14), ADVANCED("advanced_drill", 80, 30, 10, 25), ROCK("rock_cutter_drill", 20, 60, 3, 20);
        final String id; final int cost, boost, machineCost; final float speed;
        Kind(String id, int cost, float speed, int boost, int machineCost) { this.id = id; this.cost = cost; this.speed = speed; this.boost = boost; this.machineCost = machineCost; }
    }
    private PoweredDrill(Properties properties, Kind kind) {
        super(properties.stacksTo(1).attributes(net.minecraft.world.item.PickaxeItem.createAttributes(net.minecraft.world.item.Tiers.WOOD, 0, -3)).component(DataComponents.TOOL, new Tool(List.of(
            Tool.Rule.deniesDrops(kind == Kind.ADVANCED ? BlockTags.INCORRECT_FOR_NETHERITE_TOOL : BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, kind.speed), Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, kind.speed)), 1, 0)), 10000, 100, kind == Kind.ADVANCED ? 512 : 32);
        this.kind = kind;
    }
    static void register(DeferredRegister.Items items) {
        for (var kind : Kind.values()) items.registerItem(kind.id, p -> new PoweredDrill(p, kind));
    }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public boolean canUse(ItemStack stack) { return ic2.api.item.ElectricEquipment.canUse(stack, kind.cost); }
    @Override public boolean canMine(ItemStack stack, BlockState state, LevelReader level, BlockPos pos) {
        return canUse(stack) && state.getDestroySpeed(level, pos) >= 0 && (!state.requiresCorrectToolForDrops() || isCorrectToolForDrops(stack, state));
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return canUse(stack) && super.isCorrectToolForDrops(stack, state); }
    @Override public void consumeUse(ItemStack stack) { ic2.api.item.ElectricEquipment.use(stack, kind.cost, null); }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canUse(stack) || kind == Kind.ROCK && !state.is(ROCK)) return 1;
        float speed = super.getDestroySpeed(stack, state);
        if (kind == Kind.ADVANCED && mode(stack).equals("area") && speed > 1) return 270;
        return state.is(BlockTags.DIRT) && mode(stack).equals("dirt") ? speed / 8 : speed;
    }
    @Override public int progressPerTick(ItemStack stack, BlockState state) {
        int boost = kind == Kind.ROCK && state != null && state.is(ROCK) ? 100 : kind.boost;
        return 1 + boost + efficiency(stack);
    }
    @Override public int machineEuPerTick(ItemStack stack) {
        int unbreaking = enchantment(stack, Enchantments.UNBREAKING);
        return Math.max(1, 1 + kind.machineCost + efficiency(stack) - 2 * unbreaking * unbreaking);
    }
    private static int efficiency(ItemStack stack) { int level = enchantment(stack, Enchantments.EFFICIENCY); return level == 0 ? 0 : level * level + 1; }
    private static int enchantment(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet().stream().filter(entry -> entry.getKey().is(key)).mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue).findFirst().orElse(0);
    }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) {
        return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.SILK_TOUCH);
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide) {
            if (state.getDestroySpeed(level, pos) != 0) ic2.api.item.ElectricEquipment.use(stack, kind.cost, owner);
            if (owner instanceof ServerPlayer player && kind == Kind.ADVANCED && mode(stack).equals("area") && !player.isShiftKeyDown() && charge(stack) >= 8 * kind.cost && miningArea.add(player.getUUID())) {
                try { mineNeighbors(stack, player, pos); } finally { miningArea.remove(player.getUUID()); }
            }
        }
        return true;
    }
    private void mineNeighbors(ItemStack stack, ServerPlayer player, BlockPos center) {
        var axis = getPlayerPOVHitResult(player.level(), player, ClipContext.Fluid.NONE).getDirection().getAxis();
        for (int first = -1; first <= 1; first++) for (int second = -1; second <= 1; second++) {
            if (first == 0 && second == 0) continue;
            var offset = switch(axis) { case X -> center.offset(0, first, second); case Y -> center.offset(first, 0, second); case Z -> center.offset(first, second, 0); };
            if (!canUse(stack) || !player.level().hasChunkAt(offset)) continue;
            var state = player.level().getBlockState(offset);
            if (!state.isAir() && state.getDestroySpeed(player.level(), offset) >= 0) player.gameMode.destroyBlock(offset);
        }
    }
    @Override public List<String> modes() { return kind == Kind.ADVANCED ? List.of("single", "area") : List.of("normal", "dirt"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) ToolModes.next(this, this, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
