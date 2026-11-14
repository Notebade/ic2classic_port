package ic2.core.equipment;

import ic2.api.item.ModularArmor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ArmorConfigurator extends Block implements net.minecraft.world.level.block.EntityBlock {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("ic2");
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final net.neoforged.neoforge.registries.DeferredBlock<ArmorConfigurator> BLOCK =
        BLOCKS.registerBlock("armor_configurator", ArmorConfigurator::new,
            BlockBehaviour.Properties.of().strength(3.5f).requiresCorrectToolForDrops());
    private static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ArmorConfiguratorStorage>> STORAGE =
        ENTITIES.register("armor_configurator", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
            .of(ArmorConfiguratorStorage::new, BLOCK.get()).build(null));

    static net.minecraft.world.level.block.entity.BlockEntityType<?> storageType() { return STORAGE.get(); }

    private ArmorConfigurator(Properties properties) { super(properties); }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        ArmorConfiguratorStorage.MENUS.register(bus);
        items.registerSimpleBlockItem(BLOCK);
    }

    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArmorConfiguratorStorage(pos, state);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof ArmorConfiguratorStorage storage) {
            net.minecraft.world.Containers.dropContents(level, pos, storage.contents);
        }
        super.onRemove(state, level, pos, replacement, moved);
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return configure(level, pos, player) ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
            : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (configure(level, pos, player)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!(level.getBlockEntity(pos) instanceof ArmorConfiguratorStorage storage) || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        if (!level.isClientSide) player.openMenu(storage);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean configure(Level level, BlockPos pos, Player player) {
        var shell = player.getOffhandItem();
        if (shell.getCount() != 1 || !(shell.getItem() instanceof ModularArmor armor) || !level.mayInteract(player, pos)) return false;
        if (level.isClientSide) return true;
        var held = player.getMainHandItem();
        if (held.isEmpty() && ic2.api.item.EquipmentControls.current(player).alt()) {
            var installed = armor.modules(shell);
            var removed = armor.removeModule(shell, installed.size() - 1, false);
            if (!removed.isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, removed);
            return true;
        }
        if (held.isEmpty()) {
            for (var installed : armor.modules(shell)) player.sendSystemMessage(installed.stack().getHoverName());
            return true;
        }
        if (!install(armor, shell, held)) player.displayClientMessage(Component.translatable("block.ic2.armor_configurator.rejected"), true);
        return true;
    }

    static boolean install(ModularArmor armor, net.minecraft.world.item.ItemStack shell, net.minecraft.world.item.ItemStack module) {
        for (var category : armor.moduleLimits(shell).keySet().stream().sorted().toList()) {
            if (armor.install(shell, category, module)) return true;
        }
        return false;
    }
}
