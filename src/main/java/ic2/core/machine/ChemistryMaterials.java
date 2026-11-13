package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.FluidCellItem;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

final class ChemistryMaterials {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, IC2.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, IC2.MOD_ID);
    static final Supplier<FluidType> WHISKY_TYPE = TYPES.register("whisky", () -> new FluidType(FluidType.Properties.create()
        .density(1000).viscosity(790).descriptionId("fluid.ic2.whisky")));
    static final Supplier<Fluid> WHISKY = FLUIDS.register("whisky", () -> new BaseFlowingFluid.Source(
        new BaseFlowingFluid.Properties(WHISKY_TYPE, ChemistryMaterials.WHISKY, ChemistryMaterials.WHISKY)));
    static Supplier<Item> FLOUR, GRIST, MUG, WHISKY_MUG, WHISKY_CELL;
    private ChemistryMaterials() {}
    static void register(IEventBus bus, DeferredRegister.Items items) {
        TYPES.register(bus); FLUIDS.register(bus);
        FLOUR = items.registerSimpleItem("flour"); GRIST = items.registerSimpleItem("whisky_grist"); MUG = items.registerSimpleItem("mug");
        WHISKY_MUG = items.registerItem("mug_whisky", WhiskyMug::new, new Item.Properties().stacksTo(1));
        WHISKY_CELL = items.registerItem("cell_whisky", p -> new FluidCellItem(p, WHISKY.get()));
        bus.addListener(ChemistryMaterials::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidCellItem.Handler(stack), WHISKY_CELL.get());
    }
    static int grade(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("age"); }
    static ItemStack whiskyMug(int year) {
        var stack = new ItemStack(WHISKY_MUG.get()); var tag = new CompoundTag(); tag.putInt("age", year);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack;
    }
    private static final class WhiskyMug extends Item {
        WhiskyMug(Properties properties) { super(properties); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            int grade = ChemistryMaterials.grade(stack);
            int years = switch (grade) { case 1 -> 10; case 2 -> 12; case 3 -> 15; case 4 -> 25; case 5 -> 50; default -> 0; };
            lines.add(net.minecraft.network.chat.Component.translatable("gui.ic2.power_whisky_years", years));
        }
    }
}
