package ic2.core.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class ArmorConfiguratorStorage extends BlockEntity implements MenuProvider {
    static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
        net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, "ic2");
    static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ArmorConfiguratorMenu>> MENU =
        MENUS.register("armor_configurator", () -> new net.minecraft.world.inventory.MenuType<>(
            (id, inventory) -> new ArmorConfiguratorMenu(id, inventory, new SimpleContainer(1)),
            net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    final SimpleContainer contents = new SimpleContainer(1) {
        @Override public boolean stillValid(Player player) {
            return !isRemoved() && player.canInteractWithBlock(getBlockPos(), 4);
        }
    };

    ArmorConfiguratorStorage(BlockPos pos, BlockState state) {
        super(ArmorConfigurator.storageType(), pos, state);
        contents.addListener(changed -> setChanged());
    }

    @Override public Component getDisplayName() { return Component.translatable("block.ic2.armor_configurator"); }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ArmorConfiguratorMenu(id, inventory, contents);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, contents.getItems(), lookup);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        var stacks = net.minecraft.core.NonNullList.withSize(contents.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, stacks, lookup);
        for (int i = 0; i < stacks.size(); i++) contents.setItem(i, stacks.get(i));
    }

}
