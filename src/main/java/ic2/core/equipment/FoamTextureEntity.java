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

public class FoamTextureEntity extends BlockEntity {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, "ic2");
    private static final java.util.function.Supplier<BlockEntityType<FoamTextureEntity>> TYPE = TYPES.register("foam_texture", () ->
        BlockEntityType.Builder.of(FoamTextureEntity::new, java.util.stream.Stream.concat(
            java.util.stream.Stream.of(ConstructionFoam.hardenedBlock()), net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof ic2.api.item.FoamTarget)).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final ModelProperty<Map<Direction, SurfaceTexture>> SURFACES = new ModelProperty<>();
    private final Map<Direction, SurfaceTexture> textures = new EnumMap<>(Direction.class);
    private long nextFoamTick;
    static void register(net.neoforged.bus.api.IEventBus bus) { TYPES.register(bus); }
    public FoamTextureEntity(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }
    protected FoamTextureEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public SurfaceTexture texture(Direction side) { return textures.get(side); }
    public boolean apply(Direction side, SurfaceTexture texture) {
        if (level == null || level.isClientSide || texture.equals(textures.get(side))) return false;
        if (getBlockState().getBlock() instanceof ic2.api.item.FoamTarget target && target.foamLevel(getBlockState()) != 2) return false;
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
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, FoamTextureEntity entity) {
        if (level.isClientSide || !(state.getBlock() instanceof ic2.api.item.FoamTarget target) || target.foamLevel(state) != 1
                || level.getGameTime() < entity.nextFoamTick) return;
        boolean nether = level.dimension().equals(net.minecraft.world.level.Level.NETHER);
        if (nether ? level.random.nextInt(5) == 0 : level.getMaxLocalRawBrightness(pos) * 6 > level.random.nextInt(1000))
            level.setBlockAndUpdate(pos, state.setValue(ic2.api.item.FoamTarget.FOAMED, 2));
        else entity.nextFoamTick = level.getGameTime() + (nether ? 20 : 500);
    }
}
