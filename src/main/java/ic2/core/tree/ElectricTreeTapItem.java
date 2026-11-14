package ic2.core.tree;

import ic2.core.item.RechargeableItem;
import ic2.api.item.ElectricEquipment;
import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

final class ElectricTreeTapItem extends RechargeableItem implements ic2.api.item.ElectricEnchantable {
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, "ic2");
    private static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> TAP =
        SOUNDS.register("tools.treetap", net.minecraft.sounds.SoundEvent::createVariableRangeEvent);

    static void registerSounds(net.neoforged.bus.api.IEventBus bus) { SOUNDS.register(bus); }

    @Override public boolean acceptsElectricEnchantment(net.minecraft.world.item.ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
        return enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)
            || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
    }

    private static final int ENERGY_PER_TAP = 50;
    private static final int MAX_LOGS = 20;
    private final boolean advanced;

    ElectricTreeTapItem(Properties properties) {
        this(properties, false);
    }

    ElectricTreeTapItem(Properties properties, boolean advanced) {
        super(properties, 10_000, 100, 32);
        this.advanced = advanced;
    }

    @Override
    public boolean canProvideEnergy() {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var face = context.getClickedFace();
        var stack = context.getItemInHand();
        var player = context.getPlayer();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, face, stack) || !ElectricEquipment.canUse(stack, ENERGY_PER_TAP)
                || !RubberLogBlock.canTap(state, face)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            if (advanced) {
                if (!harvest(server, context)) return InteractionResult.FAIL;
            }
            else {
                ((RubberLogBlock) state.getBlock()).tap(server, pos, state, face, context.getItemInHand());
                ElectricEquipment.use(stack, ENERGY_PER_TAP, player);
            }
            server.playSound(null, pos, TAP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean harvest(ServerLevel level, UseOnContext context) {
        var queue = new ArrayDeque<BlockPos>();
        var seen = new HashSet<BlockPos>();
        var origin = context.getClickedPos();
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        queue.add(origin);
        seen.add(origin);
        int scanned = 0;
        boolean harvested = false;
        while (!queue.isEmpty() && scanned < MAX_LOGS && ElectricEquipment.canUse(stack, ENERGY_PER_TAP)) {
            var pos = queue.remove();
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof RubberLogBlock)) continue;
            scanned++;
            var face = context.getClickedFace();
            var removal = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos, state, player);
            if (level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, stack)
                    && !net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(removal).isCanceled()
                    && level.removeBlock(pos, false)) {
                harvested = true;
                int resin = RubberLogBlock.resinAmount(level, stack, 5, 10);
                if (RubberLogBlock.canTap(state, face)
                        && (state.getValue(RubberLogBlock.COLLECTABLE) || level.random.nextInt(5) == 0)) {
                    resin += RubberLogBlock.resinAmount(level, stack, 1, 3);
                }
                var result = new net.minecraft.world.item.ItemStack(ic2.core.IC2.STICKY_RESIN.get(), resin);
                player.getInventory().placeItemBackInInventory(result);
                ElectricEquipment.use(stack, ENERGY_PER_TAP, player);
            }
            for (var direction : Direction.values()) {
                var next = pos.relative(direction);
                if (seen.add(next)) queue.add(next);
            }
        }
        return harvested;
    }
}
