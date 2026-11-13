package ic2.core.equipment;

import ic2.api.item.ElectricEnchantable;
import ic2.core.item.RechargeableItem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public final class PoweredChainsaw extends RechargeableItem implements ElectricEnchantable {
    private final boolean advanced;
    private final Set<UUID> felling = new HashSet<>();
    PoweredChainsaw(Properties properties, boolean advanced) {
        super(properties.stacksTo(1).attributes(net.minecraft.world.item.AxeItem.createAttributes(net.minecraft.world.item.Tiers.WOOD, 1, 0)).component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, advanced ? 16 : 8)), 1, 0)), 10000, 100, advanced ? 512 : 32);
        this.advanced = advanced;
    }
    private int cost() { return advanced ? 100 : 50; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) { return ic2.api.item.ElectricEquipment.canUse(stack, cost()) && ability == ItemAbilities.AXE_STRIP; }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) {
        return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.FORTUNE) || enchantment.is(Enchantments.UNBREAKING);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return ic2.api.item.ElectricEquipment.canUse(stack, cost()) && (state.is(Blocks.COBWEB) || super.isCorrectToolForDrops(stack, state)); }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!ic2.api.item.ElectricEquipment.canUse(stack, cost())) return 1;
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES) || state.is(Blocks.BAMBOO) || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock ? 12 : super.getDestroySpeed(stack, state);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof IShearable target) || !ic2.api.item.ElectricEquipment.canUse(stack, 2 * cost()) || !target.isShearable(player, stack, entity.level(), entity.blockPosition())) return InteractionResult.PASS;
        if (!entity.level().isClientSide) {
            for (var drop : target.onSheared(player, stack, entity.level(), entity.blockPosition())) entity.spawnAtLocation(drop, 1);
            ic2.api.item.ElectricEquipment.use(stack, 2 * cost(), player);
        }
        return InteractionResult.sidedSuccess(entity.level().isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos(); var stack = context.getItemInHand();
        if (player == null || !ic2.api.item.ElectricEquipment.canUse(stack, cost()) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.PASS;
        var stripped = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (stripped == null) return InteractionResult.PASS;
        if (!level.isClientSide && level.setBlock(pos, stripped, 11)) ic2.api.item.ElectricEquipment.use(stack, cost(), player);
        level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1, 1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            boolean powered = ic2.api.item.ElectricEquipment.use(stack, cost(), player);
            target.hurt(player.damageSources().playerAttack(player), powered ? 10 : 1);
        }
        return false;
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide) {
            if (state.getDestroySpeed(level, pos) != 0) ic2.api.item.ElectricEquipment.use(stack, cost(), owner);
            if (advanced && state.is(BlockTags.LOGS) && owner instanceof ServerPlayer player && !player.isShiftKeyDown() && felling.add(player.getUUID())) {
                try { fellTree(stack, player, pos, state); } finally { felling.remove(player.getUUID()); }
            }
        }
        return true;
    }
    private void fellTree(ItemStack stack, ServerPlayer player, BlockPos origin, BlockState originState) {
        var queue = new ArrayDeque<BlockPos>(); var visited = new HashSet<BlockPos>(); var logs = new ArrayList<BlockPos>();
        queue.add(origin); boolean leaves = false;
        while (!queue.isEmpty() && visited.size() < 200) {
            var pos = queue.removeFirst();
            if (!visited.add(pos) || !player.level().hasChunkAt(pos) || pos.distManhattan(origin) > 32
                || Math.abs(pos.getX() - origin.getX()) > 20 || Math.abs(pos.getY() - origin.getY()) > 20 || Math.abs(pos.getZ() - origin.getZ()) > 20) continue;
            var state = player.level().getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock && state.getValue(LeavesBlock.DISTANCE) == 1) { leaves = true; continue; }
            if (!state.is(originState.getBlock())) continue;
            if (!pos.equals(origin)) logs.add(pos);
            for (var direction : Direction.values()) queue.add(pos.relative(direction));
        }
        if (leaves) for (var pos : logs) {
            if (!ic2.api.item.ElectricEquipment.canUse(stack, cost())) break;
            player.gameMode.destroyBlock(pos);
        }
    }
    public static List<ItemStack> harvestDrops(ItemStack stack, BlockState state, List<ItemStack> original) {
        if (stack.getItem() instanceof PoweredChainsaw saw && ic2.api.item.ElectricEquipment.canUse(stack, saw.cost()) && (state.is(BlockTags.LEAVES) || state.is(Blocks.COBWEB))) return List.of(new ItemStack(state.getBlock()));
        return original;
    }
}
