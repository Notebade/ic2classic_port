package ic2.core.equipment;

import ic2.api.item.ArmorModule;
import ic2.api.item.ElectricEquipment;
import ic2.api.item.EquipmentControls;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class HelmetLight extends Item implements ArmorModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final net.neoforged.neoforge.registries.DeferredBlock<TemporaryLight> LIGHT =
        BLOCKS.registerBlock("helmet_light", TemporaryLight::new,
            // An isAir state is discarded in empty 1.21 chunk sections, so use a replaceable invisible light.
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().replaceable().noCollission().noOcclusion().noLootTable().lightLevel(state -> 15));
    private HelmetLight(Properties properties) { super(properties); }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        items.registerItem("light_module", HelmetLight::new);
    }

    @Override public net.minecraft.resources.ResourceLocation category(ItemStack stack) { return GENERIC; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) { return slot == EquipmentSlot.HEAD; }

    @Override public void removed(ItemStack module, ItemStack armor) {
        CustomData.update(DataComponents.CUSTOM_DATA, armor, tag -> tag.remove("light"));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("key.sneak").append(" + ")
            .append(net.minecraft.network.chat.Component.translatable("key.ic2.hud")).append(" + ")
            .append(net.minecraft.network.chat.Component.translatable("key.ic2.mode")).append(": ")
            .append(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.light.name")));
    }

    @Override public void tickModule(ItemStack module, ItemStack armor, Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        var input = EquipmentControls.current(player);
        boolean pressed = input.hud() && input.mode() && player.isShiftKeyDown();
        var state = armor.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("light");
        boolean enabled = state.getBoolean("enabled");
        if (pressed && level.getGameTime() - state.getLong("last") >= 5) {
            state.putBoolean("enabled", !enabled);
            state.putLong("last", level.getGameTime());
            CustomData.update(DataComponents.CUSTOM_DATA, armor, tag -> tag.put("light", state));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(enabled
                ? "tooltip.item.ic2.light.disable" : "tooltip.item.ic2.light.enable"), false);
        }
        var pos = player.blockPosition().above();
        if (!enabled || level.getGameTime() % 5 != 0 || !level.mayInteract(player, pos)
                || !ElectricEquipment.canUse(armor, 10)) return;
        var block = level.getBlockState(pos);
        BlockState next;
        if (block.is(LIGHT.get())) {
            if (block.getValue(TemporaryLight.RENEWED)) return;
            next = block.setValue(TemporaryLight.RENEWED, true);
        } else if (block.isAir()) next = LIGHT.get().defaultBlockState();
        else if (block.is(net.minecraft.world.level.block.Blocks.WATER))
            next = LIGHT.get().defaultBlockState().setValue(TemporaryLight.WATER, true);
        else return;
        if (level.setBlockAndUpdate(pos, next)) ElectricEquipment.use(armor, 10, player);
    }

    private static final class TemporaryLight extends net.minecraft.world.level.block.AirBlock {
        static final net.minecraft.world.level.block.state.properties.BooleanProperty RENEWED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("renewed");
        static final net.minecraft.world.level.block.state.properties.BooleanProperty WATER =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
        TemporaryLight(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(RENEWED, false).setValue(WATER, false));
        }
        @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(RENEWED, WATER);
        }
        @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
            return state.getValue(WATER) ? net.minecraft.world.level.material.Fluids.WATER.defaultFluidState()
                : net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();
        }
        @Override protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moving) {
            if (!old.is(this) && !level.isClientSide) level.scheduleTick(pos, this, 10);
        }
        @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
            return net.minecraft.world.level.block.RenderShape.INVISIBLE;
        }
        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            state = level.getBlockState(pos);
            if (!state.is(this)) return;
            if (state.getValue(RENEWED)) {
                level.setBlock(pos, state.setValue(RENEWED, false), 50);
                level.scheduleTick(pos, this, 10);
            } else level.setBlock(pos, (state.getValue(WATER) ? net.minecraft.world.level.block.Blocks.WATER
                : net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(), 50);
        }
    }
}
