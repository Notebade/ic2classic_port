package ic2.core.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

final class WindWorldData extends SavedData {
    private WindField.State state;
    private long lastTick = Long.MIN_VALUE;
    private WindWorldData(long seed) { state = WindField.initial(seed); setDirty(); }
    private static WindWorldData get(ServerLevel level) {
        var factory = new SavedData.Factory<>(() -> new WindWorldData(level.getSeed() ^ level.dimension().location().hashCode()), WindWorldData::load, null);
        return level.getDataStorage().computeIfAbsent(factory, "ic2_wind");
    }
    static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) get(level).advance(level.getGameTime());
    }
    private void advance(long time) {
        if (time == lastTick) return;
        lastTick = time; state = state.step(); setDirty();
    }
    static double speed(ServerLevel level) { return Math.abs(get(level).state.current()); }
    private static WindWorldData load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data = new WindWorldData(tag.getLong("seed"));
        try { data.state = new WindField.State(tag.getDouble("current"), tag.getDouble("target"), tag.getInt("remaining"), tag.getLong("seed")); }
        catch (IllegalArgumentException invalid) { /* Reset an invalid saved state to a bounded wind field. */ }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putDouble("current", state.current()); tag.putDouble("target", state.target());
        tag.putInt("remaining", state.remaining()); tag.putLong("seed", state.seed());
        return tag;
    }
}
