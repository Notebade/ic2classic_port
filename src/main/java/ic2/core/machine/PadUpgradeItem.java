package ic2.core.machine;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class PadUpgradeItem extends Item {
    enum Kind {
        DAMAGE("damage_conversion"), DRAIN("drain_conversion"), PROXIMITY("proximity_booster"),
        WIDE_BAND("wide_band_booster"), ARMOR("armor_priority"), FIELD_1("basic_field_expansion"),
        FIELD_2("field_expansion"), FIELD_3("advanced_field_expansion");
        final String id;
        Kind(String id) { this.id = id + "_pad_upgrade"; }
    }
    static final Map<Kind, DeferredItem<PadUpgradeItem>> ITEMS = new EnumMap<>(Kind.class);
    final Kind kind;

    PadUpgradeItem(Properties properties, Kind kind) { super(properties); this.kind = kind; }

    static void register(DeferredRegister.Items items) {
        for (var kind : Kind.values()) ITEMS.put(kind, items.registerItem(kind.id, p -> new PadUpgradeItem(p, kind)));
    }
}
