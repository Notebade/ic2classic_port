package ic2.core.equipment;

import ic2.api.event.RetextureEvent;
import ic2.api.item.*;
import ic2.core.item.RechargeableItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

public final class ObscuratorItem extends RechargeableItem implements ConfigurableTool, ElectricEnchantable {
    private static final String SAMPLE = "ic2_surface";
    static java.util.function.Function<UseOnContext, InteractionResult> clientCapture = context -> InteractionResult.PASS;
    static java.util.function.BooleanSupplier clientMode = () -> false;
    static void register(net.neoforged.bus.api.IEventBus bus, net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        items.registerItem("obscurator", ObscuratorItem::new);
        bus.addListener(ObscuratorItem::packets);
    }
    private static void packets(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Copy.TYPE, Copy.CODEC, (packet, context) -> {
            if (packet.side < 0 || packet.side > 5) return;
            var hand = packet.offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            capture(context.player(), hand, packet.pos, Direction.from3DDataValue(packet.side), packet.colors);
        });
    }
    public record Copy(BlockPos pos, int side, boolean offhand, List<Integer> colors) implements CustomPacketPayload {
        private static final Type<Copy> TYPE = new Type<>(ResourceLocation.parse("ic2:obscurator_copy"));
        private static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Copy> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Copy::pos, ByteBufCodecs.VAR_INT, Copy::side,
            ByteBufCodecs.BOOL, Copy::offhand, ByteBufCodecs.INT.apply(ByteBufCodecs.list(10)), Copy::colors, Copy::new);
        @Override public Type<Copy> type() { return TYPE; }
    }
    ObscuratorItem(Properties properties) { super(properties.stacksTo(1), 100000, 250, 128); }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public List<String> modes() { return List.of("1", "3", "5"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String value) { return ToolModes.write(this, this, stack, value); }
    public static SurfaceTexture sample(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(SAMPLE) ? SurfaceTexture.CODEC.parse(NbtOps.INSTANCE, tag.get(SAMPLE)).result().orElse(null) : null;
    }
    private static void sample(ItemStack stack, SurfaceTexture sample) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(SAMPLE, SurfaceTexture.CODEC.encodeStart(NbtOps.INSTANCE, sample).getOrThrow()));
    }
    /** Shared server path for the validated client copy packet and addon callers. */
    public static boolean capture(Player player, InteractionHand hand, BlockPos pos, Direction side, List<Integer> colors) {
        var level = player.level(); var stack = player.getItemInHand(hand);
        if (level.isClientSide || !(stack.getItem() instanceof ObscuratorItem) || colors.isEmpty() || colors.size() > 10
                || !player.canInteractWithBlock(pos, 1) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                || !ElectricEquipment.canUse(stack, 10000)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) return false;
        var previous = sample(stack);
        var next = new SurfaceTexture(state, side, colors, java.util.Collections.nCopies(colors.size(), 0));
        if (previous != null && previous.sameSource(next)) return true;
        if (previous == null && !player.isCreative() && !ElectricEquipment.use(stack, 10000, player)) return false;
        sample(stack, next); CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ic2_surface_layer", 0));
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var input = EquipmentControls.current(player); var sample = sample(stack);
            if (input.mode()) {
                if (sample == null) return InteractionResultHolder.fail(stack);
                int layer = layer(stack, sample);
                if (player.isShiftKeyDown()) {
                    int next = (layer + 1) % sample.colors().size();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ic2_surface_layer", next));
                    player.displayClientMessage(Component.translatable("tooltip.item.ic2.obscurator.layer", next), false);
                } else {
                    sample(stack, sample.rotate(layer));
                    player.displayClientMessage(Component.translatable("tooltip.item.ic2.obscurator.rotation", sample(stack).rotations().get(layer) * 90), false);
                }
            } else if (player.isShiftKeyDown()) {
                ToolModes.next(this, this, stack);
                player.displayClientMessage(Component.translatable("tooltip.item.ic2.obscurator.range", mode(stack)), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    private static int layer(ItemStack stack, SurfaceTexture sample) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ic2_surface_layer"), 0, sample.colors().size() - 1);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel();
        if (player == null || (level.isClientSide ? clientMode.getAsBoolean() : EquipmentControls.current(player).mode())) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) return level.isClientSide ? clientCapture.apply(context) : InteractionResult.SUCCESS;
        var sample = sample(stack);
        if (sample == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int radius = Integer.parseInt(mode(stack)) / 2; boolean changed = false;
        var sides = EquipmentControls.current(player).alt() ? List.of(Direction.values()) : List.of(context.getClickedFace());
        for (int a = -radius; a <= radius; a++) for (int b = -radius; b <= radius; b++) {
            var pos = switch (context.getClickedFace().getAxis()) {
                case X -> context.getClickedPos().offset(0, a, b);
                case Y -> context.getClickedPos().offset(a, 0, b);
                case Z -> context.getClickedPos().offset(a, b, 0);
            };
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) continue;
            for (var side : sides) {
                if (!ElectricEquipment.canUse(stack, 1500)) return changed ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                var event = new RetextureEvent(level, pos, side, player, sample);
                if (NeoForge.EVENT_BUS.post(event).isCanceled()) continue;
                if (!event.isApplied() && level.getBlockEntity(pos) instanceof FoamTextureEntity foam) event.setApplied(foam.apply(side, sample));
                if (event.isApplied()) {
                    changed = true;
                    if (!player.isCreative()) ElectricEquipment.use(stack, 1500, player);
                    player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
                }
            }
        }
        return changed ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
        return enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.item.ic2.painter.range", mode(stack)));
        var sample = sample(stack);
        if (sample != null) {
            tooltip.add(Component.translatable("tooltip.item.ic2.obscurator.block", sample.state().getBlock().getName()));
            tooltip.add(Component.translatable("tooltip.item.ic2.obscurator.side", sample.side().getName()));
            tooltip.add(Component.translatable("tooltip.item.ic2.obscurator.layers", sample.colors().size()));
            tooltip.add(Component.translatable("tooltip.item.ic2.obscurator.layer", layer(stack, sample)));
            tooltip.add(Component.translatable("tooltip.item.ic2.obscurator.rotation", sample.rotations().get(layer(stack, sample)) * 90));
        }
    }
}
