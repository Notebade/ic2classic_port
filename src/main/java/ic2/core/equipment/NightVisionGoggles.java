package ic2.core.equipment;

import ic2.api.item.*;
import ic2.core.item.EuCharge;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NightVisionGoggles extends ArmorItem implements ElectricItem, ConfigurableTool, ArmorTickItem, ArmorModule {
    private NightVisionGoggles(Holder<ArmorMaterial> material, Properties properties) { super(material, Type.HELMET, properties.stacksTo(1)); }
    static void register(DeferredRegister.Items items) {
        var material = EquipmentMaterials.simple("nightvision_goggles", Type.HELMET, 0);
        items.registerItem("nightvision_goggles", p -> new NightVisionGoggles(material, p));
    }
    @Override public int capacity() { return 20000; }
    @Override public int transferRate() { return 200; }
    @Override public int minimumPacket() { return 32; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public int charge(ItemStack stack) { return EuCharge.get(stack, capacity()); }
    @Override public void setCharge(ItemStack stack, int amount) { EuCharge.set(stack, capacity(), amount); }
    @Override public List<String> modes() { return List.of("off", "on"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return super.use(level, player, hand);
        var stack = player.getItemInHand(hand); if (!level.isClientSide) ToolModes.next(this, this, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public ResourceLocation category(ItemStack module) { return GENERIC; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) { return slot == EquipmentSlot.HEAD; }
    @Override public void tickArmor(ItemStack stack, Player wearer, EquipmentSlot slot) { if (slot == EquipmentSlot.HEAD) tick(stack, stack, wearer, true); }
    @Override public void tickModule(ItemStack module, ItemStack armor, Player wearer) { tick(module, armor, wearer, false); }
    private void tick(ItemStack modeStack, ItemStack energyStack, Player wearer, boolean refill) {
        var input = EquipmentControls.current(wearer); long now = wearer.level().getGameTime();
        var data = modeStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (input.hud() && input.mode() && !wearer.isShiftKeyDown() && wearer.containerMenu == wearer.inventoryMenu
            && now >= data.getLong("ic2_night_toggle")) {
            ToolModes.next(this, this, modeStack);
            data = modeStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); data.putLong("ic2_night_toggle", now + 20);
            modeStack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        if (!mode(modeStack).equals("on")) return;
        var power = (ElectricItem) energyStack.getItem();
        if (refill) { if (!ElectricEquipment.use(energyStack, 1, wearer)) return; }
        else { if (power.charge(energyStack) < 1) return; power.setCharge(energyStack, power.charge(energyStack) - 1); }
        wearer.removeEffect(MobEffects.BLINDNESS); wearer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 250, 0, true, false));
    }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * charge(stack) / capacity()); }
    @Override public int getBarColor(ItemStack stack) { return 0x42CDEA; }
}
