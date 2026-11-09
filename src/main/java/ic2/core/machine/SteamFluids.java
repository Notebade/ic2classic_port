package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

final class SteamFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, IC2.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, IC2.MOD_ID);
    private static final TagKey<Fluid> STEAM_TAG = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "steam"));
    private static final Supplier<FluidType> TYPE = TYPES.register("steam",
        () -> new FluidType(FluidType.Properties.create().density(-100).temperature(373)));
    private static final Supplier<Fluid> STEAM = FLUIDS.register("steam",
        () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(TYPE, SteamFluids::steam, SteamFluids::steam)));

    private SteamFluids() {}

    static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    static Fluid steam() {
        return STEAM.get();
    }

    static FluidType type() {
        return TYPE.get();
    }

    static boolean isSteam(FluidStack stack) {
        return stack.is(steam()) || stack.is(STEAM_TAG);
    }
}
