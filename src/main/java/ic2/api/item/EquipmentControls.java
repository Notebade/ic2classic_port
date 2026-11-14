package ic2.api.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Call on the server thread. Input belongs only to this player and expires after twenty ticks without refresh. */
public final class EquipmentControls {
    private static final String KEY = "ic2_equipment_input";
    private EquipmentControls() {}
    public static boolean update(Player player, EquipmentInput input) {
        if (player.level().isClientSide || !player.isAlive()) return false;
        var data = new CompoundTag(); data.putInt("mask", input.mask()); data.putLong("tick", player.level().getGameTime());
        player.getPersistentData().put(KEY, data); return true;
    }
    public static EquipmentInput current(Player player) {
        if (player.level().isClientSide || !player.isAlive()) return EquipmentInput.NONE;
        var data = player.getPersistentData().getCompound(KEY); long age = player.level().getGameTime() - data.getLong("tick");
        if (age < 0 || age > 20) return EquipmentInput.NONE;
        int mask = data.getInt("mask"); return (mask & ~EquipmentInput.KNOWN_MASK) == 0 ? EquipmentInput.fromMask(mask) : EquipmentInput.NONE;
    }
    public static void clear(Player player) { player.getPersistentData().remove(KEY); }
}
