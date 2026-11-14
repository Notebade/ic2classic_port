package ic2.core.equipment;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;

/** Original three scrolling glint layers on the player model. */
final class EnergyShieldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    EnergyShieldLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override public void render(PoseStack poses, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float swing, float amount, float partialTick, float age, float yaw, float pitch) {
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ModularArmor armor)) return;
        var features = armor.features(helmet);
        if (!features.contains(ArmorModule.id("energy_shield_overlay"))) return;
        boolean always = features.contains(ArmorModule.id("energy_shield_always"));
        if (!always && player.hurtTime == 0) return;
        int time = always ? 0 : Math.abs(player.hurtTime);
        float visibility = time == 0 || time >= 8 ? 1 : time / 7F;
        try {
            for (int pass = 0; pass < 3; pass++) {
                var type = ShieldType.layer(pass, player.tickCount * .015F);
                RenderSystem.setShaderColor(.245F * visibility, .708F * visibility, .803F * visibility, 1);
                getParentModel().renderToBuffer(poses, buffers.getBuffer(type), 4194368, OverlayTexture.NO_OVERLAY);
                if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch(type);
            }
        } finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
    }

    private static final class ShieldType extends RenderType {
        private static final ResourceLocation TEXTURE = ResourceLocation.parse("ic2:textures/models/misc/electric_shield_texture.png");
        private ShieldType(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                boolean delegate, boolean sorting, Runnable setup, Runnable clear) {
            super(name, format, mode, size, delegate, sorting, setup, clear);
        }
        static RenderType layer(int pass, float time) {
            var texture = new TexturingStateShard("ic2_shield_scroll", () -> {
                float angle = (float) Math.toRadians(pass * 25);
                float offset = time * (.001F + pass * .005F) * 20;
                RenderSystem.setTextureMatrix(new Matrix4f().scaling(2).rotateZ(angle)
                    .translate(-(float) Math.sin(angle) * offset, (float) Math.cos(angle) * offset, 0));
            }, RenderSystem::resetTextureMatrix);
            return create("ic2_energy_shield", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false,
                CompositeState.builder().setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(TEXTURE, true, false)).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(texture).setOutputState(MAIN_TARGET).createCompositeState(false));
        }
    }
}
