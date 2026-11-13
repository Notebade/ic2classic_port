package ic2.core.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.core.IC2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class MonitorRenderer implements BlockEntityRenderer<MonitorEntity> {

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(NuclearInfrastructure.MONITOR_ENTITY.get(), (c)->new MonitorRenderer());
    }

    @Override
    public void render(MonitorEntity entity, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!entity.master || entity.lines.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        pose.translate(0, 0, 0.502);
        float scale = entity.scale / 256F;
        pose.translate(-0.45, 0.45, 0);
        pose.scale(scale, -scale, scale);
        int width = (int) ((entity.width - 0.1F) / scale);
        int height = (int) ((entity.height - 0.1F) / scale);
        int y = 0;
        int color = 0xff000000 | net.minecraft.world.item.DyeColor.byId(entity.color).getTextColor();
        for (var line : entity.lines) for (var part : font.split(line.copy().withStyle(style -> style.withColor(color & 0xffffff)), width)) {
            if (y + font.lineHeight > height) break;
            int x = MonitorLayout.textX(width, font.width(part), entity.alignment);
            font.drawInBatch(part, x, y, color, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, 15728880);
            y += 10;
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MonitorEntity entity) { return true; }
}
