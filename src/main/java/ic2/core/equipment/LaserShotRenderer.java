package ic2.core.equipment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class LaserShotRenderer extends EntityRenderer<LaserShot> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("ic2:textures/models/misc/mining_laser_shot.png");

    LaserShotRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override public void render(LaserShot shot, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, shot.yRotO, shot.getYRot()) - 90));
        poses.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, shot.xRotO, shot.getXRot())));
        poses.mulPose(Axis.XP.rotationDegrees(45));
        poses.scale(.05625f, .05625f, .05625f);
        poses.translate(-4, 0, 0);
        var vertices = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        var cap = poses.last();
        vertex(vertices, cap, -7, -2, -2, 0, .15625f, -1, 0, light);
        vertex(vertices, cap, -7, -2, 2, .15625f, .15625f, -1, 0, light);
        vertex(vertices, cap, -7, 2, 2, .15625f, .3125f, -1, 0, light);
        vertex(vertices, cap, -7, 2, -2, 0, .3125f, -1, 0, light);
        vertex(vertices, cap, -7, 2, -2, 0, .15625f, 1, 0, light);
        vertex(vertices, cap, -7, 2, 2, .15625f, .15625f, 1, 0, light);
        vertex(vertices, cap, -7, -2, 2, .15625f, .3125f, 1, 0, light);
        vertex(vertices, cap, -7, -2, -2, 0, .3125f, 1, 0, light);
        for (int face = 0; face < 4; face++) {
            poses.mulPose(Axis.XP.rotationDegrees(90));
            var pose = poses.last();
            vertex(vertices, pose, -8, -2, 0, 0, 0, 0, 1, light);
            vertex(vertices, pose, 8, -2, 0, .5f, 0, 0, 1, light);
            vertex(vertices, pose, 8, 2, 0, .5f, .15625f, 0, 1, light);
            vertex(vertices, pose, -8, 2, 0, 0, .15625f, 0, 1, light);
        }
        poses.popPose();
        super.render(shot, yaw, partial, poses, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, float nx, float ny, int light) {
        consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, 0);
    }

    @Override public ResourceLocation getTextureLocation(LaserShot shot) {
        return TEXTURE;
    }
}
