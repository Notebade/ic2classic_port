package ic2.core.machine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class HeatPumpItem extends Item {
    private static final String DIRECTIONS = "heat_pump_directions";

    public HeatPumpItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    static int directions(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(DIRECTIONS) & 15;
    }

    static void toggle(ItemStack stack, int direction) {
        int mask = directions(stack) ^ (1 << direction);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(DIRECTIONS, mask));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new HeatPumpMenu(id, held, hand), held.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }
}
