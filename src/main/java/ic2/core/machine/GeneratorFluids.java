package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.FluidCellItem;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class GeneratorFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, IC2.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, IC2.MOD_ID);
    enum Fuel {
        WOODGAS("woodgas", "cell_woodgas", 250, 10, "woodgas_still", 0xffffffff),
        BIO("bio_fuel", "cell_fluid_bio_fuel", 2000, 128, "base_fluid_still", 0xffff6f00),
        ALCOHOL("alcohol", "cell_fluid_alcohol", 24000, 50, "base_fluid_still", 0xff9daa6a);
        final String id, cellId, texture;
        final int ticks, output, tint;
        Supplier<FluidType> type; Supplier<Fluid> fluid; Supplier<Item> cell;
        Fuel(String id, String cellId, int ticks, int output, String texture, int tint) {
            this.id = id; this.cellId = cellId; this.ticks = ticks; this.output = output; this.texture = texture; this.tint = tint;
        }
    }
    private GeneratorFluids() {}
    static void register(IEventBus bus, DeferredRegister.Items items) {
        for (Fuel fuel : Fuel.values()) {
            fuel.type = TYPES.register(fuel.id, () -> new FluidType(FluidType.Properties.create()
                .density(fuel == Fuel.WOODGAS ? -1000 : 1000).viscosity(fuel == Fuel.BIO ? 790 : 500)
                .descriptionId("fluid.ic2." + fuel.id)));
            fuel.fluid = FLUIDS.register(fuel.id, () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(fuel.type, fuel.fluid, fuel.fluid)));
            fuel.cell = items.registerItem(fuel.cellId, p -> new FluidCellItem(p, fuel.fluid.get()));
        }
        TYPES.register(bus); FLUIDS.register(bus);
        bus.addListener(GeneratorFluids::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (Fuel fuel : Fuel.values()) event.registerItem(Capabilities.FluidHandler.ITEM,
            (stack, ignored) -> new FluidCellItem.Handler(stack), fuel.cell.get());
    }
    static Fuel fuel(Fluid fluid) {
        for (Fuel fuel : Fuel.values()) if (fuel.fluid.get() == fluid) return fuel;
        return null;
    }
    public static ItemStack filledCell(Fluid fluid) {
        Fuel fuel = fuel(fluid); return fuel == null ? ItemStack.EMPTY : new ItemStack(fuel.cell.get());
    }
}
