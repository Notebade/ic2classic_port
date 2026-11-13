package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricEnchantable;
import ic2.core.item.RechargeableItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

final class PoweredMower extends RechargeableItem implements ConfigurableTool, ElectricEnchantable {
    private final Set<UUID> harvesting = new HashSet<>();
    PoweredMower(Properties properties) {
        super(properties.stacksTo(1).attributes(net.minecraft.world.item.HoeItem.createAttributes(net.minecraft.world.item.Tiers.WOOD, 2, -3)), 10000, 100, 32);
    }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public List<String> modes() { return List.of("off", "on"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) { return ability == ItemAbilities.AXE_SCRAPE && ic2.api.item.ElectricEquipment.canUse(stack, 1) && mode(stack).equals("on"); }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) {
        return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.UNBREAKING);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) ToolModes.next(this, this, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, net.minecraft.world.entity.LivingEntity owner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0 && !harvesting.contains(owner.getUUID())) ic2.api.item.ElectricEquipment.use(stack, 1, owner);
        return true;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected) {
        if (!(owner instanceof ServerPlayer player) || level.getGameTime() % 5 != 0 || !ic2.api.item.ElectricEquipment.canUse(stack, 1) || !mode(stack).equals("on")) return;
        if (!selected && player.getOffhandItem() != stack) return;
        var ray = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (ray.getType() == HitResult.Type.BLOCK) mowAround(stack, player, ray);
        else strikeEntity(stack, player);
    }
    private void mowAround(ItemStack stack, ServerPlayer player, BlockHitResult ray) {
        var center = ray.getBlockPos(); var level = player.serverLevel();
        for (var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, ray.getDirection(), stack)) continue;
            var state = level.getBlockState(pos);
            var context = new UseOnContext(player, player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), ray.getDirection(), pos, false));
            var scraped = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
            if (scraped != null) { level.setBlock(pos, scraped, 3); continue; }
            if (!ic2.api.item.ElectricEquipment.canUse(stack, 5) || state.getBlock() instanceof CropBlock || !(state.is(BlockTags.LEAVES) || state.getBlock() instanceof BushBlock)) continue;
            harvesting.add(player.getUUID());
            try {
                if (player.gameMode.destroyBlock(pos) && !level.getBlockState(pos).equals(state)) ic2.api.item.ElectricEquipment.use(stack, 5, player);
            } finally { harvesting.remove(player.getUUID()); }
        }
    }
    private void strikeEntity(ItemStack stack, ServerPlayer player) {
        var start = player.getEyePosition(); var end = start.add(player.getLookAngle().scale(5));
        Entity nearest = null; double distance = start.distanceToSqr(end);
        for (var target : player.level().getEntities(player, player.getBoundingBox().expandTowards(player.getLookAngle().scale(5)).inflate(1), Entity::isPickable)) {
            var hit = target.getBoundingBox().inflate(target.getPickRadius()).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < distance) { nearest = target; distance = start.distanceToSqr(hit.get()); }
        }
        if (nearest != null && nearest.hurt(player.damageSources().generic(), 1)) ic2.api.item.ElectricEquipment.use(stack, 1, player);
    }
    static void collectDrops(BlockDropsEvent event) {
        if (!(event.getTool().getItem() instanceof PoweredMower mower) || !(event.getBreaker() instanceof ServerPlayer player) || !mower.harvesting.contains(player.getUUID())) return;
        var iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            var entity = iterator.next(); player.getInventory().add(entity.getItem());
            if (entity.getItem().isEmpty()) iterator.remove();
        }
    }
}
