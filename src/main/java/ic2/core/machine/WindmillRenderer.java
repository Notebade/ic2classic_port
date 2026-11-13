package ic2.core.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.core.IC2;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Original four-blade geometry and UV layout, with one cached mesh per rotor radius. */
@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class WindmillRenderer implements BlockEntityRenderer<PowerMachineEntity> {
    private final java.util.Map<Integer,ModelPart> models=new java.util.HashMap<>();
    private static ModelPart model(int radius){
        int size=radius*8+2;var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("shaft",CubeListBuilder.create().texOffs(29,0).addBox(0,-2,-2,10,4,4),PartPose.offset(-14,0,0));
        var blade=root.addOrReplaceChild("blade",CubeListBuilder.create(),PartPose.ZERO);
        blade.addOrReplaceChild("wing",CubeListBuilder.create().texOffs(48,14).addBox(-2,2,-2,4,size+12,4),PartPose.offset(-12,0,0));
        blade.addOrReplaceChild("fan",CubeListBuilder.create().addBox(-1,14,-18,2,size,16),PartPose.offset(-12,0,0));
        return LayerDefinition.create(mesh,64,128).bakeRoot();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PowerMachines.ENTITY.get(), context -> new WindmillRenderer());
    }
    @Override public boolean shouldRenderOffScreen(PowerMachineEntity entity) { return entity.windmill != null; }
    @Override public void render(PowerMachineEntity entity, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (entity.windmill == null || entity.getLevel() == null) return;
        var stack = entity.inventory.getStackInSlot(0);
        if (!(stack.getItem() instanceof WindRotor rotor)) return;
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(Axis.YP.rotationDegrees(90 - entity.front().toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(entity.windmill.angle(entity.getLevel().getGameTime() + partial)));
        pose.translate(-.25,0,0);
        String texture=switch(rotor.kind){case WOODEN->"wood";case ALLOY->"alloy";default->rotor.kind.id;};
        var consumer=buffers.getBuffer(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath("ic2","textures/models/windmill/"+texture+"_rotor.png")));
        var model=models.computeIfAbsent(rotor.kind.radius,WindmillRenderer::model);
        model.getChild("shaft").render(pose,consumer,light,overlay);
        for(int i=0;i<4;i++){pose.mulPose(Axis.XP.rotationDegrees(90));model.getChild("blade").render(pose,consumer,light,overlay);}
        pose.popPose();
    }
}
