package ic2.core.machine;

import ic2.core.IC2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Registration only: all processor rendering lives in ProcessingMachineScreen. */
@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class AdvancedMachineScreen {
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event) {
        event.register(AdvancedMachines.MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.ROTARY_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.CENTRIFUGAL_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.SINGULARITY_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.RARE_EARTH_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.COMPACTING_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.SLOW_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
        event.register(AdvancedMachines.PRESSURE_MENU.get(), ProcessingMachineScreen<AdvancedMachineMenu>::new);
    }
}
