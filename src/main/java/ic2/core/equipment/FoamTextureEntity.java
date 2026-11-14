package ic2.core.equipment;

import ic2.api.item.SurfaceTexture;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FoamTextureEntity extends BlockEntity {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final java.util.function.Supplier<BlockEntityType<FoamTextureEntity>> TYPE = TYPES.register("foam_texture", () ->
        BlockEntityType.Builder.of(FoamTextureEntity::new, ConstructionFoam.hardenedBlock()).build(null));
    public static final ModelProperty<Map<Direction, SurfaceTexture>> SURFACES = new ModelProperty<>();
    private final Map<Direction, SurfaceTexture> textures = new EnumMap<>(Direction.class);
    static void register(net.neoforged.bus.api.IEventBus bus) { TYPES.register(bus); }
    public FoamTextureEntity(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
    public SurfaceTexture texture(Direction side) { return textures.get(side); }
    public boolean apply(Direction side, SurfaceTexture texture) {
        if (level == null || level.isClientSide || texture.equals(textures.get(side))) return false;
        textures.put(side, texture); setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        textures.forEach((side, texture) -> tag.put(side.getName(), SurfaceTexture.CODEC.encodeStart(NbtOps.INSTANCE, texture).getOrThrow()));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); textures.clear();
        for (var side : Direction.values()) if (tag.contains(side.getName()))
            SurfaceTexture.CODEC.parse(NbtOps.INSTANCE, tag.get(side.getName())).result().ifPresent(texture -> textures.put(side, texture));
        if (level != null && level.isClientSide) { requestModelDataUpdate(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public ModelData getModelData() { return ModelData.builder().with(SURFACES, Map.copyOf(textures)).build(); }
}
