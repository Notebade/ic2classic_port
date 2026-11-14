package ic2.core.equipment;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ObscuratorClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        ObscuratorItem.clientMode = EquipmentKeys::modeDown;
        ObscuratorItem.clientCapture = context -> {
            var client = Minecraft.getInstance(); var state = context.getLevel().getBlockState(context.getClickedPos());
            var model = client.getBlockRenderer().getBlockModel(state);
            if (!ic2.api.item.ElectricEquipment.canUse(context.getItemInHand(), 10000) || model.isCustomRenderer()
                || model == client.getModelManager().getMissingModel()) return InteractionResult.FAIL;
            var quads = model.getQuads(state, context.getClickedFace(), RandomSource.create(state.getSeed(context.getClickedPos())), ModelData.EMPTY, null);
            if (quads.isEmpty() || quads.size() > 10) return InteractionResult.FAIL;
            var colors = quads.stream().map(quad -> quad.isTinted()
                ? client.getBlockColors().getColor(state, context.getLevel(), context.getClickedPos(), quad.getTintIndex()) : -1).toList();
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ObscuratorItem.Copy(context.getClickedPos(),
                context.getClickedFace().get3DDataValue(), context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND, colors));
            return InteractionResult.SUCCESS;
        };
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        var foam = ResourceLocation.parse("ic2:construction_foam");
        var obscurator = ResourceLocation.parse("ic2:obscurator");
        event.getModels().replaceAll((id, model) -> {
            boolean coveredCable = id.variant().contains("foamed=2")
                && net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id.id()) instanceof ic2.api.item.FoamTarget;
            return id.id().equals(foam) || coveredCable ? new FoamModel(model)
                : id.id().equals(obscurator) ? new ToolModel(model) : model;
        });
    }
    private static BakedQuad surfaceQuad(BakedQuad source, Direction side, int rotation, int tint, org.joml.Vector3f min, org.joml.Vector3f max) {
        var face = new BlockElementFace(null, -1, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, rotation * 90));
        var quad = new FaceBakery().bakeQuad(min, max, face, source.getSprite(), side, BlockModelRotation.X0_Y0, null, source.isShade());
        int color = tint | 0xFF000000;
        int abgr = color & 0xFF00FF00 | (color >>> 16 & 255) | (color & 255) << 16;
        int[] vertices = quad.getVertices(); int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) vertices[vertex * stride + 3] = abgr;
        return quad;
    }
    private static final class ToolModel extends BakedModelWrapper<BakedModel> {
        private final java.util.Map<ic2.api.item.SurfaceTexture, BakedModel> previews = new java.util.LinkedHashMap<>(128, .75F, true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<ic2.api.item.SurfaceTexture, BakedModel> eldest) { return size() > 128; }
        };
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, net.minecraft.world.item.ItemStack stack,
                    net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) {
                var texture = ObscuratorItem.sample(stack);
                return texture == null ? model : previews.computeIfAbsent(texture, sample -> new PreviewModel(originalModel, sample));
            }
        };
        ToolModel(BakedModel original) { super(original); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }
    private static final class PreviewModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> quads;
        PreviewModel(BakedModel original, ic2.api.item.SurfaceTexture sample) {
            super(original);
            var result = new java.util.ArrayList<>(original.getQuads(null, null, RandomSource.create(0)));
            var source = Minecraft.getInstance().getBlockRenderer().getBlockModel(sample.state())
                .getQuads(sample.state(), sample.side(), RandomSource.create(sample.state().getSeed(net.minecraft.core.BlockPos.ZERO)), ModelData.EMPTY, null);
            for (int layer = 0; layer < Math.min(source.size(), sample.colors().size()); layer++)
                for (var side : List.of(Direction.NORTH, Direction.SOUTH)) result.add(surfaceQuad(source.get(layer), side,
                    sample.rotations().get(layer), sample.colors().get(layer), new org.joml.Vector3f(2, 2, 7.498F), new org.joml.Vector3f(10, 10, 8.502F)));
            quads = List.copyOf(result);
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
            return face == null ? quads : originalModel.getQuads(state, face, random);
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random, ModelData data, RenderType type) {
            return getQuads(state, face, random);
        }
        @Override public List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return List.of(this); }
    }
    private static final class FoamModel extends BakedModelWrapper<BakedModel> {
        FoamModel(BakedModel original) { super(original); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random, ModelData data, RenderType type) {
            var surfaces = data.get(FoamTextureEntity.SURFACES);
            var texture = surfaces == null || face == null ? null : surfaces.get(face);
            if (texture == null) return type != null && !originalModel.getRenderTypes(state, random, data).contains(type)
                ? List.of() : originalModel.getQuads(state, face, random, data, type);
            var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(texture.state());
            if (type != null && !model.getRenderTypes(texture.state(), random, ModelData.EMPTY).contains(type)) return List.of();
            var originals = model.getQuads(texture.state(), texture.side(), RandomSource.create(0), ModelData.EMPTY, null);
            var result = new java.util.ArrayList<BakedQuad>();
            for (int index = 0; index < Math.min(originals.size(), texture.colors().size()); index++) {
                result.add(surfaceQuad(originals.get(index), face, texture.rotations().get(index), texture.colors().get(index),
                    new org.joml.Vector3f(), new org.joml.Vector3f(16, 16, 16)));
            }
            return result.isEmpty() ? originalModel.getQuads(state, face, random, data, type) : result;
        }
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
            var types = new java.util.ArrayList<ChunkRenderTypeSet>(); types.add(originalModel.getRenderTypes(state, random, data));
            var surfaces = data.get(FoamTextureEntity.SURFACES);
            if (surfaces != null) for (var texture : surfaces.values()) types.add(Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(texture.state()).getRenderTypes(texture.state(), random, ModelData.EMPTY));
            return ChunkRenderTypeSet.union(types);
        }
    }
}
