package ic2.core.equipment;

import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

final class TankModule extends Item implements ArmorModule {
    private static final ResourceLocation TANK = ArmorModule.id("armor_tank");
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, "ic2");
    private static final java.util.function.Supplier<DataComponentType<SimpleFluidContent>> CONTENT =
        COMPONENTS.registerComponentType("armor_tank", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private TankModule(Properties properties) { super(properties); }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        COMPONENTS.register(bus);
        items.registerItem("tank_storage_module", TankModule::new);
        bus.addListener(TankModule::registerCapability);
    }

    private static void registerCapability(RegisterCapabilitiesEvent event) {
        for (var id : new String[]{"nano_suit_chestplate", "quantum_suit_chestplate"}) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ArmorModule.id(id));
            event.registerItem(Capabilities.FluidHandler.ITEM, (armor, ignored) ->
                ((ModularArmor) armor.getItem()).features(armor).contains(TANK) ? new Handler(armor) : null, item);
        }
    }

    @Override public ResourceLocation category(ItemStack module) { return STORAGE; }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST && !((ModularArmor) armor.getItem()).features(armor).contains(TANK);
    }
    @Override public Set<ResourceLocation> features(ItemStack module, ItemStack armor) { return Set.of(TANK); }

    static boolean containsFluid(ItemStack armor) {
        return !armor.getOrDefault(CONTENT, SimpleFluidContent.EMPTY).isEmpty();
    }

    static void appendTooltip(ItemStack armor, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        if (!(armor.getItem() instanceof ModularArmor item) || !item.features(armor).contains(TANK)) return;
        var fluid = armor.getOrDefault(CONTENT, SimpleFluidContent.EMPTY).copy();
        tooltip.add(fluid.isEmpty() ? net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.tank_storage_module.empty", 0, 64000)
            : net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.tank_storage_module.filled", fluid.getAmount(), 64000, fluid.getHoverName()));
    }

    private static final class Handler extends FluidHandlerItemStack {
        Handler(ItemStack stack) { super(CONTENT, stack, 64_000); }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return ((ModularArmor) container.getItem()).features(container).contains(TANK) ? super.fill(resource, action) : 0;
        }
    }
}
