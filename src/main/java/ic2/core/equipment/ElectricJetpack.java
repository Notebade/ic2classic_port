package ic2.core.equipment;

import ic2.api.item.ArmorTickItem;
import ic2.api.item.ElectricEquipment;
import ic2.api.item.ElectricItem;
import ic2.api.item.EquipmentControls;
import ic2.core.item.EuCharge;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

final class ElectricJetpack extends ArmorItem implements ElectricItem, ArmorTickItem, JetpackFlight.Pack, ic2.api.item.ArmorModule, ic2.api.item.ElectricEnchantable {
    private final boolean compact;
    private final boolean nuclear;

    static void register(net.neoforged.neoforge.registries.DeferredRegister.Items items) {
        register(items, "electric_jetpack", false, false);
        register(items, "compacted_electric_jetpack", true, false);
        register(items, "nuclear_jetpack", false, true);
        register(items, "compacted_nuclear_jetpack", true, true);
    }

    private static void register(net.neoforged.neoforge.registries.DeferredRegister.Items items, String id, boolean compact, boolean nuclear) {
        var material = EquipmentMaterials.simple(id, Type.CHESTPLATE, 0);
        items.registerItem(id, p -> new ElectricJetpack(material, p, compact, nuclear));
    }

    private ElectricJetpack(net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, Properties properties, boolean compact, boolean nuclear) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1));
        this.compact = compact;
        this.nuclear = nuclear;
    }

    @Override public int capacity() { return compact ? 360_000 : 30_000; }
    @Override public int transferRate() { return nuclear ? 0 : compact ? 500 : 60; }
    @Override public int minimumPacket() { return compact && !nuclear ? 128 : 32; }
    @Override public boolean canProvideEnergy() { return false; }
    @Override public int charge(ItemStack stack) { return EuCharge.get(stack, capacity()); }
    @Override public void setCharge(ItemStack stack, int amount) { EuCharge.set(stack, capacity(), amount); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * charge(stack) / capacity()); }
    @Override public int getBarColor(ItemStack stack) { return 0x42CDEA; }
    @Override public java.util.List<String> modes() { return compact || nuclear ? java.util.List.of("normal", "hover", "advanced_hover") : java.util.List.of("normal", "hover"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean acceptsElectricEnchantment(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, boolean anvil) {
        return !enchantment.is(net.minecraft.world.item.enchantment.Enchantments.MENDING)
            && (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING) || enchantment.value().isSupportedItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE)));
    }
    @Override public int electricEnchantability(ItemStack stack) { return 10; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        JetpackFlight.tooltip(this, stack, tooltip);
        if (nuclear) {
            var data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            tooltip.add(net.minecraft.network.chat.Component.translatable("key.ic2.inv").append(" + ")
                .append(net.minecraft.network.chat.Component.translatable("key.mouse.right")).append(": ")
                .append(net.minecraft.network.chat.Component.translatable("tooltip.ic2.open_item_inventory")));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.jetpack.heat.reactor", data.getInt("reactor_heat"), data.getInt("reactor_max")));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.jetpack.heat.component", data.getInt("reactor_component_heat") + "%"));
        }
    }

    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (nuclear && (level.isClientSide ? ModularSuitItem.clientSideInventory.getAsBoolean() : EquipmentControls.current(player).sideInventory())) {
            if (!level.isClientSide) NuclearJetpackMenu.open(player, stack, hand, -1);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }
    @Override public void tickArmor(ItemStack stack, Player wearer, EquipmentSlot slot) {
        if (slot != EquipmentSlot.CHEST || wearer.level().isClientSide) return;
        JetpackFlight.tick(this, stack, stack, wearer);
        if (nuclear) ic2.core.machine.PortableReactor.tick(stack, stack, wearer);
    }
    @Override public JetpackFlight.Profile flightProfile() {
        if (nuclear) return compact
            ? new JetpackFlight.Profile(true, true, 1.8, .05, 1.1, 30000, 40, 35, 1, 2, 3)
            : new JetpackFlight.Profile(true, true, .95, .05, 1.1, 25000, 12, 9, .5, 1, 1.5);
        return compact ? new JetpackFlight.Profile(true, true, 1.4, .05, 1.1, 25000, 30, 25, .6, 1.2, 1.8)
            : new JetpackFlight.Profile(true, true, .7, .05, 1.28, 0, 7, 4, .3, .65, .65);
    }
    @Override public int maxFuel() { return capacity(); }
    @Override public int fuel(ItemStack stack) { return ((ElectricItem) stack.getItem()).charge(stack); }
    @Override public void spend(ItemStack stack, Player wearer, int amount) {
        if (!nuclear && stack.is(this) && ElectricEquipment.use(stack, amount, wearer)) return;
        var power = (ElectricItem) stack.getItem();
        power.setCharge(stack, Math.max(0, power.charge(stack) - amount));
    }
    @Override public net.minecraft.resources.ResourceLocation category(ItemStack module) { return BACK; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) { return slot == EquipmentSlot.CHEST; }
    @Override public void tickModule(ItemStack module, ItemStack armor, Player player) {
        JetpackFlight.tick(this, module, armor, player);
        if (nuclear) ic2.core.machine.PortableReactor.tick(module, armor, player);
    }
    @Override public boolean useOnArmor(ItemStack module, ItemStack armor, Player player) {
        if (!nuclear || !(player.level().isClientSide ? ModularSuitItem.clientSideInventory.getAsBoolean()
                : EquipmentControls.current(player).sideInventory())) return false;
        if (!player.level().isClientSide) {
            var entries = ArmorModules.read(armor);
            for (int index = 0; index < entries.size(); index++) if (entries.get(index).stack().is(this)) {
                var hand = player.getMainHandItem() == armor ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
                NuclearJetpackMenu.open(player, armor, hand, index); break;
            }
        }
        return true;
    }}
