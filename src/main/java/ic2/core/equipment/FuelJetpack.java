package ic2.core.equipment;

import ic2.api.item.ArmorTickItem;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

final class FuelJetpack extends ArmorItem implements ArmorTickItem, JetpackFlight.Pack {
    private static final int CAPACITY = 18_000;
    private final boolean compact;
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, "ic2");
    private static final java.util.function.Supplier<net.minecraft.core.component.DataComponentType<SimpleFluidContent>> CONTENT =
        COMPONENTS.registerComponentType("jetpack_fuel", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    static void register(IEventBus bus, DeferredRegister.Items items) {
        COMPONENTS.register(bus);
        register(bus, items, "fuel_jetpack", false);
        register(bus, items, "compact_fuel_jetpack", true);
    }
    private static void register(IEventBus bus, DeferredRegister.Items items, String id, boolean compact) {
        var material = EquipmentMaterials.simple(id, Type.CHESTPLATE, 0);
        var pack = items.registerItem(id, p -> new FuelJetpack(material, p, compact));
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ignored) -> handler(stack), pack.get()));
    }

    private FuelJetpack(Holder<ArmorMaterial> material, Properties properties, boolean compact) {
        super(material, Type.CHESTPLATE, properties.stacksTo(1));
        this.compact = compact;
    }

    private static FluidHandlerItemStack handler(ItemStack stack) {
        return new FluidHandlerItemStack(CONTENT, stack, ((FuelJetpack) stack.getItem()).capacity()) {
            @Override public boolean canFillFluidType(net.neoforged.neoforge.fluids.FluidStack resource) {
                return isFluidValid(0, resource);
            }
            @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack resource) {
                return net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(resource.getFluid())
                    .equals(net.minecraft.resources.ResourceLocation.parse("ic2:bio_fuel"));
            }
        };
    }
    private int capacity() { return compact ? 1_080_118 : CAPACITY; }

    @Override public List<String> modes() { return compact ? java.util.List.of("normal", "hover", "advanced_hover") : java.util.List.of("normal", "hover"); }
    @Override public String mode(ItemStack stack) { return ToolModes.read(this, stack); }
    @Override public boolean setMode(ItemStack stack, String mode) { return ToolModes.write(this, this, stack, mode); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * handler(stack).getFluid().getAmount() / capacity()); }
    @Override public int getBarColor(ItemStack stack) { return 0xFF6F00; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) { JetpackFlight.tooltip(this, stack, tooltip); }

    @Override public void tickArmor(ItemStack stack, Player wearer, EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST && !wearer.level().isClientSide) JetpackFlight.tick(this, stack, stack, wearer);
    }
    @Override public JetpackFlight.Profile flightProfile() {
        return compact ? new JetpackFlight.Profile(false, true, 1.2, .1, 1, 20000, 35, 30, .6, 1, 1.4)
            : new JetpackFlight.Profile(false, false, 1, .5, 1, 0, 9, 6, .15, .5, .5);
    }
    @Override public int fuel(ItemStack stack) { return handler(stack).getFluid().getAmount(); }
    @Override public int maxFuel() { return capacity() + 2; }
    @Override public void spend(ItemStack stack, Player player, int amount) { handler(stack).drain(amount, FluidAction.EXECUTE); }
}
