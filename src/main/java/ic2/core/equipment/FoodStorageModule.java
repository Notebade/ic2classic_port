package ic2.core.equipment;

import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

final class FoodStorageModule extends Item implements ArmorModule {
    private static final ResourceLocation FEATURE = ArmorModule.id("food_storage");
    private static final ResourceLocation EMPTY = ArmorModule.id("tin_can");

    FoodStorageModule(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public ResourceLocation category(ItemStack module) { return STORAGE; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST && !enabled(armor);
    }

    @Override public java.util.Set<ResourceLocation> features(ItemStack module, ItemStack armor) {
        return java.util.Set.of(FEATURE);
    }

    static boolean enabled(ItemStack armor) {
        return armor.getItem() instanceof ModularArmor modular && modular.features(armor).contains(FEATURE);
    }

    static boolean hasContents(ItemStack armor) {
        return armor.getOrDefault(ArmorConfiguration.FOOD.get(), ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext();
    }

    static boolean feed(Player player, ItemStack helmet, ItemStack chest) {
        if (!enabled(chest)) return false;
        var items = load(chest);
        for (int slot = 0; slot < 2; slot++) {
            var stack = items.get(slot);
            if (stack.isEmpty()) continue;
            var food = stack.getFoodProperties(player);
            if (food == null || !ic2.api.item.ElectricEquipment.use(helmet, 1000, player)) return false;
            var remainder = stack.split(1).finishUsingItem(player.level(), player);
            for (int output = 2; output < 4 && !remainder.isEmpty(); output++) {
                var stored = items.get(output);
                if (stored.isEmpty()) {
                    items.set(output, remainder);
                    remainder = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(stored, remainder) && stored.getCount() < stored.getMaxStackSize()) {
                    stored.grow(1);
                    remainder.shrink(1);
                }
            }
            chest.set(ArmorConfiguration.FOOD.get(), ItemContainerContents.fromItems(items));
            if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
            return true;
        }
        return false;
    }

    static InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand, ItemStack armor) {
        if (level.isClientSide) return InteractionResultHolder.success(armor);
        var contents = new SimpleContainer(4) {
            @Override public boolean canPlaceItem(int slot, ItemStack stack) {
                var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                return slot < 2 ? stack.getItem() instanceof FoodCans : slot < 4 && id.equals(EMPTY);
            }

            @Override public boolean stillValid(Player viewer) {
                return viewer.getItemInHand(hand) == armor && enabled(armor);
            }
        };
        var items = load(armor);
        for (int slot = 0; slot < 4; slot++) contents.setItem(slot, items.get(slot));
        contents.addListener(changed -> armor.set(ArmorConfiguration.FOOD.get(), ItemContainerContents.fromItems(
            java.util.stream.IntStream.range(0, 4).mapToObj(contents::getItem).toList())));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new FoodStorageMenu(id, inventory, contents, armor, hand),
                Component.translatable("item.ic2.food_storage_module")), buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
        }
        return InteractionResultHolder.consume(armor);
    }

    private static NonNullList<ItemStack> load(ItemStack armor) {
        var items = NonNullList.withSize(4, ItemStack.EMPTY);
        armor.getOrDefault(ArmorConfiguration.FOOD.get(), ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }
}
