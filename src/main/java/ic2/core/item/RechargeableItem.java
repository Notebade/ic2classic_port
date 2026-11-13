package ic2.core.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class RechargeableItem extends Item implements ic2.api.item.ElectricItem {
    private final int capacity;
    private final int transferRate;
    private final int minimumPacket;

    public RechargeableItem(Properties properties, int capacity, int transferRate, int minimumPacket) {
        super(properties);
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.minimumPacket = minimumPacket;
    }

    public static boolean supports(ItemStack stack, int machinePacket) {
        return supports(stack, machinePacket, true);
    }

    public static boolean supports(ItemStack stack, int machinePacket, boolean charging) {
        return ic2.api.item.ElectricItem.supports(stack, machinePacket, charging);
    }

    public boolean canProvideEnergy() {
        return true;
    }

    public int capacity() {
        return capacity;
    }

    public int transferRate() {
        return transferRate;
    }

    public final int minimumPacket() {
        return minimumPacket;
    }

    public int charge(ItemStack stack) {
        return EuCharge.get(stack, capacity);
    }

    public void setCharge(ItemStack stack, int charge) {
        EuCharge.set(stack, capacity, charge);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * charge(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x42CDEA;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ic2.re_battery.charge", charge(stack), capacity)
            .withStyle(ChatFormatting.GRAY));
    }
}
