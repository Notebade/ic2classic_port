package ic2.core.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.core.IC2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Rotating item-sprite rotor; no collision volume is added around the controller. */
@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class WindmillRenderer implements BlockEntityRenderer<PowerMachineEntity> {
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
        pose.mulPose(Axis.YP.rotationDegrees(180 - entity.front().toYRot()));
        pose.translate(0, 0, -.55);
        pose.mulPose(Axis.ZP.rotationDegrees(entity.windmill.angle(entity.getLevel().getGameTime() + partial)));
        pose.scale(rotor.kind.radius, rotor.kind.radius, 1);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
            pose, buffers, entity.getLevel(), entity.getBlockPos().hashCode());
        pose.popPose();
    }
}
