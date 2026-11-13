package ic2.api.item;

/** Input intent only. Equipment still validates its own slot, energy, cooldown and world conditions. */
public record EquipmentInput(boolean jump, boolean boost, boolean forward, boolean hud, boolean mode, boolean alt) {
    public static final EquipmentInput NONE = new EquipmentInput(false, false, false, false, false, false);
    public int mask() { return (jump ? 1 : 0) | (boost ? 2 : 0) | (forward ? 4 : 0) | (hud ? 8 : 0) | (mode ? 16 : 0) | (alt ? 32 : 0); }
    public static EquipmentInput fromMask(int value) {
        if ((value & ~63) != 0) throw new IllegalArgumentException("Unknown equipment input bits");
        return new EquipmentInput((value & 1) != 0, (value & 2) != 0, (value & 4) != 0, (value & 8) != 0, (value & 16) != 0, (value & 32) != 0);
    }
}
