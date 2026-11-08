package ic2.core.item;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ReBatteryItem extends Item {
    public static final int CAPACITY = 10_000;
    public static final int TRANSFER_PER_TICK = 100;

    public ReBatteryItem(Properties properties) {
        super(properties);
    }

    public static boolean isBattery(ItemStack stack) {
        return stack.is(IC2.RE_BATTERY.get()) && stack.getCount() == 1;
    }

    public static int charge(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(IC2.BATTERY_CHARGE.get(), 0), 0, CAPACITY);
    }

    public static void setCharge(ItemStack stack, int charge) {
        if (charge == 0) {
            stack.remove(IC2.BATTERY_CHARGE.get());
        } else {
            stack.set(IC2.BATTERY_CHARGE.get(), charge);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x42CDEA;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ic2.re_battery.charge", charge(stack), CAPACITY)
            .withStyle(ChatFormatting.GRAY));
    }
}
