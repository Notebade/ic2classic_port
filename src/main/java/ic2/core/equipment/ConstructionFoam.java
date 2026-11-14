package ic2.core.equipment;

import ic2.api.item.*;
import ic2.core.item.RechargeableItem;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ConstructionFoam {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final net.neoforged.neoforge.registries.DeferredBlock<FoamTextureBlock> HARDENED =
        BLOCKS.registerBlock("construction_foam", FoamTextureBlock::new, BlockBehaviour.Properties.of().strength(2));
    static Block hardenedBlock() { return HARDENED.get(); }
    private static final net.neoforged.neoforge.registries.DeferredBlock<Wet> WET =
        BLOCKS.registerBlock("wet_construction_foam", Wet::new, BlockBehaviour.Properties.of().strength(.1f, 10)
            .sound(net.minecraft.world.level.block.SoundType.WOOL).noCollission().noOcclusion().randomTicks());
    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        FoamTextureEntity.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, ConstructionFoam::checkTarget);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, ConstructionFoam::placeTarget);
        items.registerSimpleBlockItem(HARDENED); items.registerSimpleBlockItem(WET);
        items.registerSimpleItem("cf_pellet");
        items.registerItem("electric_sprayer", p -> new Sprayer(p, true));
        items.registerItem("cf_sprayer", p -> new Sprayer(p, false));
        var material = EquipmentMaterials.simple("cf_pack", net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 0);
        items.registerItem("cf_pack", p -> new Pack(material, p));
    }
    private static void checkTarget(ic2.api.event.FoamEvent.Check event) {
        if (!event.isCustomPlacement() && event.getState().getBlock() instanceof FoamTarget target && target.foamLevel(event.getState()) == 0)
            event.setCustomTarget(target.foamTargetType());
    }
    private static void placeTarget(ic2.api.event.FoamEvent.Place event) {
        if (event.getLevel() instanceof ServerLevel server && event.getState().getBlock() instanceof FoamTarget target
                && target.applyFoam(server, event.getPos())) event.setCanceled(true);
    }
    private static final class Wet extends Block {
        Wet(Properties properties) { super(properties); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { tick(state, level, pos, random); }
        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean nether = level.dimension().equals(net.minecraft.world.level.Level.NETHER);
            if (nether ? random.nextInt(5) == 0 : level.getMaxLocalRawBrightness(pos) * 6 > random.nextInt(1000))
                level.setBlockAndUpdate(pos, HARDENED.get().defaultBlockState());
            else level.scheduleTick(pos, this, nether ? 40 : 500);
        }
        @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
                net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
            if (!stack.is(net.minecraft.tags.ItemTags.SAND)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, HARDENED.get().defaultBlockState());
                if (!player.isCreative()) stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
    }
    private static final class Pack extends net.minecraft.world.item.ArmorItem implements FoamContainer {
        Pack(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Properties properties) { super(material, Type.CHESTPLATE, properties.durability(3400)); }
        @Override public int foamCapacity(ItemStack stack) { return 3400; }
        @Override public int storedFoam(ItemStack stack) { return Math.max(0, 3400 - stack.getDamageValue()); }
        @Override public void setFoam(ItemStack stack, int amount) { stack.setDamageValue(3400 - Math.clamp(amount, 0, 3400)); }
        @Override public boolean suppliesWornFoam() { return true; }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
    }
    private static final class Sprayer extends RechargeableItem implements ConfigurableTool, FoamContainer, ElectricEnchantable {
        private static final List<String> MODES = List.of("1", "2", "4", "8", "16", "32");
        private final boolean electric;
        Sprayer(Properties properties, boolean electric) {
            super(electric ? properties.stacksTo(1) : properties.durability(1600), electric ? 10000 : 0, electric ? 100 : 0, 32);
            this.electric = electric;
        }
        @Override public boolean canProvideEnergy() { return false; }
        @Override public int foamCapacity(ItemStack stack) { return electric ? 1000 : 1600; }
        @Override public int storedFoam(ItemStack stack) {
            return electric ? Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ic2_foam"), 0, 1000)
                : Math.max(0, 1600 - stack.getDamageValue());
        }
        @Override public void setFoam(ItemStack stack, int amount) {
            int stored = Math.clamp(amount, 0, foamCapacity(stack));
            if (electric) CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ic2_foam", stored));
            else stack.setDamageValue(1600 - stored);
        }
        @Override public int refillAmount() { return electric ? 26 : 100; }
        @Override public boolean canRefill(ItemStack stack) { return electric ? FoamContainer.super.canRefill(stack) : stack.isDamaged(); }
        @Override public List<String> modes() { return electric ? MODES : List.of("13"); }
        @Override public String mode(ItemStack stack) {
            int index = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("foam_mode");
            return electric ? MODES.get(Math.clamp(index, 0, MODES.size() - 1)) : "13";
        }
        @Override public boolean setMode(ItemStack stack, String mode) {
            if (!electric || !MODES.contains(mode)) return false;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("foam_mode", MODES.indexOf(mode)));
            return true;
        }
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
                net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (electric && EquipmentControls.current(player).mode() && !level.isClientSide) {
                setMode(stack, MODES.get((MODES.indexOf(mode(stack)) + 1) % MODES.size()));
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("info.item.ic2.electric_sprayer.range", mode(stack)), false);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player = context.getPlayer(); var level = context.getLevel(); var stack = context.getItemInHand();
            if (player == null || !player.mayBuild() || EquipmentControls.current(player).mode()) return InteractionResult.PASS;
            int limit = Integer.parseInt(mode(stack));
            if (electric && !ElectricEquipment.canUse(stack, limit * 5)) return InteractionResult.FAIL;
            var provider = provider(player, stack, limit);
            if (provider.isEmpty()) return InteractionResult.FAIL;
            var origin = new net.minecraft.world.item.context.BlockPlaceContext(context).getClickedPos();
            var initial = check(context, origin);
            if (initial == null) return InteractionResult.FAIL;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            var queue = new ArrayDeque<BlockPos>(); var visited = new HashSet<BlockPos>();
            var directions = directions(player); queue.add(origin);
            var selected = new LinkedHashSet<BlockPos>();
            while (!queue.isEmpty() && selected.size() < limit) {
                var pos = queue.removeFirst();
                if (!visited.add(pos)) continue;
                var check = pos.equals(origin) ? initial : check(context, pos);
                if (check == null || initial.getType() != ic2.api.event.FoamEvent.TargetType.ANY && check.getType() != initial.getType()) continue;
                selected.add(pos);
                var spread = check.isCustomPlacement() ? new ArrayList<>(List.of(Direction.values())) : new ArrayList<>(directions);
                Collections.shuffle(spread, new Random(server.random.nextLong()));
                for (var direction : spread) queue.addLast(pos.relative(direction));
            }
            int placed = 0;
            for (var pos : selected) {
                var snapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(server.dimension(), server, pos);
                var placement = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ic2.api.event.FoamEvent.Place(server, pos));
                if ((!placement.isCanceled() || placement.shouldPlaceFoam()) && !server.setBlockAndUpdate(pos, WET.get().defaultBlockState())) continue;
                var event = new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(snapshot,
                    server.getBlockState(pos.relative(context.getClickedFace().getOpposite())), player);
                if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) { snapshot.restore(); continue; }
                placed++;
            }
            if (placed == 0) return InteractionResult.FAIL;
            var foam = (FoamContainer) provider.getItem();
            if (provider != stack || !player.isCreative()) foam.setFoam(provider, foam.storedFoam(provider) - placed);
            if (electric) ElectricEquipment.use(stack, placed * 5, player);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }
        private static ic2.api.event.FoamEvent.Check check(UseOnContext context, BlockPos pos) {
            var level = context.getLevel(); var player = context.getPlayer();
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return null;
            var check = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ic2.api.event.FoamEvent.Check(level, pos));
            if (check.isCanceled()) return null;
            var state = level.getBlockState(pos);
            return check.isCustomPlacement() || state.isAir() || state.is(Blocks.FIRE) || state.liquid() ? check : null;
        }
        private ItemStack provider(net.minecraft.world.entity.player.Player player, ItemStack sprayer, int amount) {
            for (var worn : player.getArmorSlots())
                if (worn.getItem() instanceof FoamContainer foam && foam.suppliesWornFoam() && foam.storedFoam(worn) >= amount) return worn;
            return storedFoam(sprayer) >= amount ? sprayer : ItemStack.EMPTY;
        }
        private static List<Direction> directions(net.minecraft.world.entity.player.Player player) {
            var directions = new ArrayList<>(List.of(Direction.values()));
            float yaw = player.getYRot() % 360, pitch = player.getXRot() % 360;
            if (pitch >= -65 && pitch <= 65) {
                if (yaw >= 300 && yaw <= 360 || yaw >= 0 && yaw <= 60) directions.remove(Direction.NORTH);
                if (yaw >= 30 && yaw <= 150) directions.remove(Direction.EAST);
                if (yaw >= 120 && yaw <= 240) directions.remove(Direction.SOUTH);
                if (yaw >= 210 && yaw <= 330) directions.remove(Direction.WEST);
            }
            if (pitch <= -40) directions.remove(Direction.DOWN);
            if (pitch >= 40) directions.remove(Direction.UP);
            return directions;
        }
        @Override public boolean acceptsElectricEnchantment(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
            return electric && enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
        }
        @Override public int getBarWidth(ItemStack stack) { return electric ? super.getBarWidth(stack) : Math.round(13F * storedFoam(stack) / 1600); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            if (electric) super.appendHoverText(stack, context, tooltip, flag);
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.ic2.foam.stored", storedFoam(stack), foamCapacity(stack)));
            if (electric) tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.electric_sprayer.range", mode(stack)));
        }
    }
}
