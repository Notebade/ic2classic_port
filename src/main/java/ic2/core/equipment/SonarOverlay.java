package ic2.core.equipment;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import ic2.api.item.ModularArmor;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Source sonar terrain wave: expanding square, 15-tick growth, 25-tick lifetime. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class SonarOverlay {
    private record Entry(BlockPos pos, long created) {}
    private static final ArrayDeque<Entry> ENTRIES = new ArrayDeque<>();
    private static BlockPos origin;
    private static long started = Long.MIN_VALUE;
    private static int radius, targetRadius;

    private static CompoundTag sonarData() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.getItem() instanceof ModularArmor armor) {
                for (var installed : armor.modules(helmet)) {
                    var module = installed.stack();
                    if (module.getItem() instanceof SonarModule)
                        return module.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                }
            }
        }
        return new CompoundTag();
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null) {
            ENTRIES.clear(); origin = null; started = Long.MIN_VALUE; radius = targetRadius = 0;
            return;
        }
        if (client.isPaused()) return;
        long now = client.level.getGameTime();
        while (!ENTRIES.isEmpty() && now - ENTRIES.getFirst().created >= 25) ENTRIES.removeFirst();
        var data = sonarData();
        if (data.contains("positions")) {
            if (data.getLong("started") != started) {
                started = data.getLong("started"); origin = BlockPos.of(data.getLong("pos"));
                radius = targetRadius = 0;
                addSurface(origin, now);
            }
            targetRadius = Math.max(targetRadius, (int) (255 * data.getDouble("progress")));
        }
        if (origin != null && radius < targetRadius) addRing(++radius, now);
    }

    private static void addRing(int distance, long now) {
        for (int x = -distance; x <= distance; x++) {
            addSurface(origin.offset(x, 0, -distance), now);
            addSurface(origin.offset(x, 0, distance), now);
        }
        for (int z = 1 - distance; z < distance; z++) {
            addSurface(origin.offset(-distance, 0, z), now);
            addSurface(origin.offset(distance, 0, z), now);
        }
    }

    private static void addSurface(BlockPos pos, long now) {
        var level = Minecraft.getInstance().level;
        if (!level.hasChunkAt(pos)) return;
        var search = pos.mutable();
        var solid = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque();
        if (solid.test(level.getBlockState(search))) {
            while (search.getY() < level.getMaxBuildHeight() && solid.test(level.getBlockState(search))) search.move(0, 1, 0);
            search.move(0, -1, 0);
        } else {
            while (search.getY() > level.getMinBuildHeight() && !solid.test(level.getBlockState(search))) search.move(0, -1, 0);
        }
        ENTRIES.addLast(new Entry(search.immutable(), now));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ENTRIES.isEmpty()) return;
        var client = Minecraft.getInstance();
        var poses = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        poses.pushPose(); poses.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            int rendered = 0;
            for (var entry : ENTRIES) {
                if (!event.getFrustum().isVisible(new AABB(entry.pos))) continue;
                int age = (int) Math.max(0, client.level.getGameTime() - entry.created);
                double inset = age >= 15 ? 0 : .5 - age / 30.0;
                var box = new AABB(entry.pos).deflate(inset);
                // Original color 0x79AAAAAA, with alpha growing to 200 over 15 ticks.
                LevelRenderer.addChainedFilledBoxVertices(poses, vertices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    170 / 255F, 170 / 255F, 170 / 255F, Math.min(age, 15) / 15F * 200 / 255F);
                if (++rendered == 2000) break;
            }
            var mesh = vertices.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        } finally {
            RenderSystem.enableCull(); RenderSystem.depthMask(true); RenderSystem.enableDepthTest(); RenderSystem.disableBlend();
            poses.popPose();
        }
    }
}
