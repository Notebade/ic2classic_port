package ic2.core.equipment;

import ic2.api.item.ConfigurableTool;
import ic2.api.item.ElectricEquipment;
import ic2.api.item.EquipmentControls;
import ic2.core.item.RechargeableItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class MiningLaser extends RechargeableItem implements ConfigurableTool {
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, "ic2");
    private static final java.util.Map<String, ? extends java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> SHOTS =
        java.util.stream.Stream.of("normal", "low_focus", "long_range", "scatter", "explosive").collect(
            java.util.stream.Collectors.toUnmodifiableMap(name -> name, name -> SOUNDS.register("tools.mininglaser." + name,
                id -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id))));

    static void register(net.neoforged.bus.api.IEventBus bus, net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        SOUNDS.register(bus);
        LaserShot.register(bus);
        items.registerItem("mining_laser", MiningLaser::new);
    }
    MiningLaser(Properties properties) { super(properties.stacksTo(1), 200_000, 120, 128); }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public List<String> modes() { return List.of("mining", "low_focus", "long_range", "horizontal", "super_heat", "scatter", "explosive", "tracking"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player wearer, InteractionHand hand) {
        var stack = wearer.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(wearer instanceof ServerPlayer player) || !player.mayBuild()) return InteractionResultHolder.pass(stack);
        if (EquipmentControls.current(player).mode()) {
            ToolModes.next(this, this, stack);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.mining_laser.mode." + mode(stack)), true);
            return InteractionResultHolder.consume(stack);
        }
        var mode = mode(stack);
        if (mode.equals("horizontal")) return InteractionResultHolder.pass(stack);
        var origin = player.getEyePosition().subtract(0, .1, 0);
        var direction = player.getLookAngle();
        if (mode.equals("tracking")) {
            var target = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, origin,
                origin.add(direction.scale(60)), player.getBoundingBox().expandTowards(direction.scale(60)).inflate(1),
                entity -> entity.isPickable() && (entity instanceof net.minecraft.world.entity.LivingEntity
                    || entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal), 3600);
            if (target == null || !player.hasLineOfSight(target.getEntity())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.mining_laser.error.no_target"), true);
                return InteractionResultHolder.pass(stack);
            }
            if (target.getEntity() instanceof Player other && !player.canHarmPlayer(other)) return InteractionResultHolder.fail(stack);
            if (!payForShot(stack, 10000, player)) return InteractionResultHolder.fail(stack);
            LaserShot.spawn(player, origin, direction, mode, target.getEntity());
            sound(player, mode);
            return InteractionResultHolder.consume(stack);
        }
        int cost = switch (mode) {
            case "low_focus" -> 100;
            case "long_range", "explosive" -> 5000;
            case "super_heat" -> 2500;
            case "scatter" -> 10000;
            default -> 1250;
        };
        if (!payForShot(stack, cost, player)) return InteractionResultHolder.fail(stack);
        if (mode.equals("scatter")) {
            for (int yaw = -2; yaw <= 2; yaw++) for (int pitch = -2; pitch <= 2; pitch++) {
                LaserShot.spawn(player, origin, Vec3.directionFromRotation(player.getXRot() + 20 * pitch, player.getYRot() + 20 * yaw), mode, null);
            }
        } else {
            LaserShot.spawn(player, origin, direction, mode, null);
        }
        sound(player, mode);
        return InteractionResultHolder.consume(stack);
    }

    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        if (!mode(stack).equals("horizontal")) return net.minecraft.world.InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player) || !player.mayBuild() || EquipmentControls.current(player).mode()) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        double y = context.getClickedPos().getY() + .5;
        var origin = player.getEyePosition().subtract(0, .1, 0);
        if (Math.abs(origin.y - y) >= 1.5) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.mining_laser.error.steep"), true);
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (!payForShot(stack, 3000, player)) return net.minecraft.world.InteractionResult.FAIL;
        var direction = Vec3.directionFromRotation(0, Math.round(player.getYRot() / 45f) * 45f);
        LaserShot.spawn(player, new Vec3(origin.x, y, origin.z), direction, "horizontal", null);
        sound(player, "horizontal");
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    private static boolean payForShot(ItemStack stack, int cost, ServerPlayer player) {
        if (player.isCreative() || ElectricEquipment.use(stack, cost, player)) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.mining_laser.no_energy"), true);
        return false;
    }

    static boolean breakBlock(ServerPlayer player, BlockPos pos, boolean smelt, Runnable exhaust) {
        var level = player.serverLevel();
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, pos) < 0) return false;
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        if (state.getBlock() instanceof net.minecraft.world.level.block.TntBlock tnt) {
            tnt.onCaughtFire(state, level, pos, null, player);
            return level.removeBlock(pos, false);
        }

        var drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, level.getBlockEntity(pos));
        var replacement = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var outputs = new java.util.ArrayList<ItemStack>();
        for (var drop : drops) {
            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(drop.copyWithCount(1));
            var recipe = smelt ? level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level)
                : java.util.Optional.<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>>empty();
            if (recipe.isEmpty()) {
                if (!smelt || !state.ignitedByLava()) outputs.add(drop);
                continue;
            }
            var output = recipe.get().value().assemble(input, level.registryAccess());
            if (output.isEmpty()) continue;
            exhaust.run();
            var block = net.minecraft.world.level.block.Block.byItem(output.getItem());
            if (block != net.minecraft.world.level.block.Blocks.AIR && block != state.getBlock()) {
                replacement = block.defaultBlockState();
            } else {
                int count = output.getCount() * drop.getCount();
                for (int remaining = count; remaining > 0; remaining -= output.getMaxStackSize()) {
                    outputs.add(output.copyWithCount(Math.min(remaining, output.getMaxStackSize())));
                }
            }
        }
        if (!level.setBlockAndUpdate(pos, replacement)) return false;
        outputs.forEach(drop -> net.minecraft.world.level.block.Block.popResource(level, pos, drop));
        return true;
    }

    private static void sound(ServerPlayer player, String mode) {
        String name = switch (mode) { case "low_focus", "long_range", "scatter", "explosive" -> mode; default -> "normal"; };
        player.serverLevel().playSound(null, player.blockPosition(), SHOTS.get(name).get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
    }
}
