package ic2.api.item;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Declarative parameters are pure and captured before installation callbacks. Callbacks may change operational state,
 * but must not change electrical modifiers. Live equipment mutations belong on the server; tickModule is server-only. */
public interface ArmorModule {
    ResourceLocation BATTERY = id("battery"), HUD = id("hud"), ANY = id("any"), GENERIC = id("generic"),
        CHARGER = id("charge"), STORAGE = id("storage"), BACK = id("back_slot"), MOVEMENT = id("movement");
    static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("ic2", name); }
    ResourceLocation category(ItemStack module);
    boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot);
    default ArmorEnergyModifier energyModifier(ItemStack module, ItemStack armor) { return ArmorEnergyModifier.NONE; }
    default boolean sharesArmorEnergy(ItemStack module, ItemStack armor) { return false; }
    default double extraStorageMultiplier(ItemStack module, ItemStack armor) { return 0; }
    default double extraTransferMultiplier(ItemStack module, ItemStack armor) { return 0; }
    default Set<ResourceLocation> features(ItemStack module, ItemStack armor) { return Set.of(); }
    default void installed(ItemStack module, ItemStack armor) {}
    default void removed(ItemStack module, ItemStack armor) {}
    default void tickModule(ItemStack module, ItemStack armor, Player wearer) {}
    /** Handles use of the held armor. Server mutations to the module are persisted by its holder. */
    default boolean useOnArmor(ItemStack module, ItemStack armor, Player wearer) { return false; }
}
