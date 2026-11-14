package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

final class ElectricWrenchItem extends RechargeableItem implements ic2.api.item.ElectricEnchantable {
    private final int energyPerUse;

    ElectricWrenchItem(Properties properties) {
        this(properties, false);
    }

    ElectricWrenchItem(Properties properties, boolean precision) {
        super(properties, precision ? 40_000 : 12_000, precision ? 350 : 250, precision ? 128 : 32);
        energyPerUse = precision ? 100 : 50;
    }

    @Override
    public boolean canProvideEnergy() {
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (energyPerUse == 50 && context.getPlayer() != null && ic2.api.item.EquipmentControls.current(context.getPlayer()).mode()) return InteractionResult.PASS;
        return WrenchItem.useOnMachine(context, cost -> ic2.api.item.ElectricEquipment.canUse(stack, cost * energyPerUse),
            cost -> ic2.api.item.ElectricEquipment.use(stack, cost * energyPerUse, context.getPlayer()), energyPerUse == 100);
    }
    boolean canOverrideLoss(ItemStack stack) {
        var data = data(stack);
        return energyPerUse == 100 ? data.getBoolean("lossless") || data.getInt("LossUses") < 30
            : data.getBoolean("losslessMode") && data.getInt("LossUses") < 15;
    }
    double recoveryMultiplier(ItemStack stack) {
        int fortune = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).entrySet().stream()
            .filter(entry -> entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE))
            .mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue).findFirst().orElse(0);
        return (energyPerUse == 100 ? 2 : 1.1) * Math.pow(1.03, fortune);
    }
    void preventedLoss(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (energyPerUse == 100 && data(stack).getBoolean("lossless")) return;
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
            int uses = Math.max(0, tag.getInt("LossUses")) + 1; tag.putInt("LossUses", uses);
            if (energyPerUse == 50 && uses >= 15) tag.putBoolean("losslessMode", false);
        });
        if (energyPerUse == 50) player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
            "tooltip.item.ic2.electric_wrench.machine_saved", Math.max(0, 15 - data(stack).getInt("LossUses"))), true);
    }
    private static net.minecraft.nbt.CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (energyPerUse == 50 && !level.isClientSide && ic2.api.item.EquipmentControls.current(player).mode() && data(stack).getInt("LossUses") < 15) {
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putBoolean("losslessMode", !tag.getBoolean("losslessMode")));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(data(stack).getBoolean("losslessMode")
                ? "tooltip.item.ic2.electric_wrench.losslessWrenchModeOn" : "tooltip.item.ic2.electric_wrench.losslessWrenchModeOff"), false);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
        return enchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
            || enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var data = data(stack);
        tooltip.add(data.getBoolean("lossless") && energyPerUse == 100
            ? net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.electric_wrench.losslessWrenchInfiniteUses")
            : net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.electric_wrench.losslessWrenchUses",
                Math.max(0, (energyPerUse == 100 ? 30 : 15) - data.getInt("LossUses"))));
        if (energyPerUse == 50) tooltip.add(net.minecraft.network.chat.Component.translatable(data.getBoolean("losslessMode")
            ? "tooltip.item.ic2.electric_wrench.losslessWrenchModeOn" : "tooltip.item.ic2.electric_wrench.losslessWrenchModeOff"));
    }
}
