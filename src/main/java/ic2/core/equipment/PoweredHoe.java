package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricEnchantable;
import ic2.core.item.RechargeableItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

final class PoweredHoe extends RechargeableItem implements ConfigurableTool, ElectricEnchantable {
    private final boolean advanced;
    PoweredHoe(Properties properties, boolean advanced) {
        super(properties.stacksTo(1).attributes(net.minecraft.world.item.HoeItem.createAttributes(net.minecraft.world.item.Tiers.WOOD, 4, -3)).component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, advanced ? 9 : 6)), 1, 0)), advanced ? 100000 : 10000, 100, advanced ? 512 : 32);
        this.advanced = advanced;
    }
    private int cost() { return advanced ? 100 : 50; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public boolean canPerformAction(ItemStack stack, ItemAbility ability) { return ic2.api.item.ElectricEquipment.canUse(stack, cost()) && ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability); }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil) {
        return enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.FORTUNE);
    }
    @Override public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0) ic2.api.item.ElectricEquipment.use(stack, cost(), owner);
        return true;
    }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return ic2.api.item.ElectricEquipment.canUse(stack, cost()) ? super.getDestroySpeed(stack, state) : 1; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var stack = context.getItemInHand();
        if (player == null || !ic2.api.item.ElectricEquipment.canUse(stack, cost())) return InteractionResult.PASS;
        int radius = advanced ? Integer.parseInt(mode(stack)) : 0;
        int changed = 0;
        // The selected block must be valid before spreading to its neighbors.
        if (!till(context, context.getClickedPos())) return InteractionResult.PASS;
        changed++;
        for (var pos : BlockPos.betweenClosed(context.getClickedPos().offset(-radius, 0, -radius), context.getClickedPos().offset(radius, 0, radius))) {
            if (!pos.equals(context.getClickedPos()) && ic2.api.item.ElectricEquipment.canUse(stack, cost()) && till(context, pos)) changed++;
        }
        return changed > 0 ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }
    private boolean till(UseOnContext original, BlockPos pos) {
        var level = original.getLevel(); var player = original.getPlayer(); var stack = original.getItemInHand();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, original.getClickedFace(), stack)) return false;
        var context = new UseOnContext(player, original.getHand(), new BlockHitResult(Vec3.atCenterOf(pos), original.getClickedFace(), pos, false));
        var state = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        if (state == null) return false;
        if (!level.isClientSide) {
            if (!level.setBlock(pos, state, 11)) return false;
            ic2.api.item.ElectricEquipment.use(stack, cost(), player);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        }
        level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1, 1);
        return true;
    }
    @Override public List<String> modes() { return advanced ? List.of("0", "1", "2", "3", "4", "5") : List.of("0"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!advanced || !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) ToolModes.next(this, this, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
