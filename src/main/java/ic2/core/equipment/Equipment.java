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
    }
}
