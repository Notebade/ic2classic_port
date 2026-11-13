package ic2.core.equipment;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Equipment {
    private Equipment() {}
    public static void register(IEventBus bus, DeferredRegister.Items items) {
        ConventionalTools.register(items);
        ConventionalArmor.register(bus, items);
        ConventionalShield.register(items);
        items.registerItem("nano_shield", NanoShield::new);
        PoweredDrill.register(items);
        items.registerItem("electric_hoe", p -> new PoweredHoe(p, false));
        items.registerItem("advanced_hoe", p -> new PoweredHoe(p, true));
        items.registerItem("chainsaw", p -> new PoweredChainsaw(p, false));
        items.registerItem("advanced_chainsaw", p -> new PoweredChainsaw(p, true));
    }
}
