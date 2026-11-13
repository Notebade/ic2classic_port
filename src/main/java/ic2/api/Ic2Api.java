package ic2.api;

import ic2.api.machine.MachineSnapshot;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** Public entry point for the IC2 Classic port. See API.md for scope and lifecycle. */
public final class Ic2Api {
    public static final String MOD_ID = "ic2";
    public static final int API_VERSION = 1;
    private Ic2Api() {}
    /** Runtime version check; unlike a constant reference this is not inlined into an addon. */
    public static int version() { return API_VERSION; }
    public static List<ResourceLocation> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(MOD_ID)).sorted().toList();
    }
    public static List<ResourceLocation> blockIds() {
        return BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals(MOD_ID)).sorted().toList();
    }
    public static List<ResourceLocation> fluidIds() {
        return BuiltInRegistries.FLUID.keySet().stream().filter(id -> id.getNamespace().equals(MOD_ID)).sorted().toList();
    }
    public static Optional<Item> item(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return BuiltInRegistries.ITEM.containsKey(id) ? Optional.of(BuiltInRegistries.ITEM.get(id)) : Optional.empty();
    }
    public static Optional<Block> block(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return BuiltInRegistries.BLOCK.containsKey(id) ? Optional.of(BuiltInRegistries.BLOCK.get(id)) : Optional.empty();
    }
    public static Optional<Fluid> fluid(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return BuiltInRegistries.FLUID.containsKey(id) ? Optional.of(BuiltInRegistries.FLUID.get(id)) : Optional.empty();
    }
    /** Server thread only. Never loads chunks; unsupported blocks return empty. */
    public static Optional<MachineSnapshot> machineInfo(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level"); Objects.requireNonNull(pos, "pos");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Read machine data on the server thread");
        if (!level.hasChunkAt(pos)) return Optional.empty();
        var entity = level.getBlockEntity(pos);
        if (entity instanceof MachineSnapshot.Provider provider) return Optional.of(provider.machineInfo());
        return entity instanceof MachineSnapshot.Access access ? access.resolveMachineInfo() : Optional.empty();
    }
}
