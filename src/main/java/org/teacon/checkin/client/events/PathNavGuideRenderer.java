package org.teacon.checkin.client.events;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.renderer.ModRenderType;
import org.teacon.checkin.configs.ClientConfig;
import org.teacon.checkin.network.capability.GuidingManager;


@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PathNavGuideRenderer {
    public static final Material NAV_ARROW_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(CheckMeIn.MODID, "item/nav_arrow"));

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var player = mc.player;
        var level = player.level();
        if (!ClientConfig.INSTANCE.pathNaviArrowAlwaysShow.get() && !player.isHolding(CheckMeIn.NVG_PATH.get())) return;

        var capOpt = GuidingManager.of(mc.player).resolve();
        if (capOpt.isPresent()) {
            var nextPos = capOpt.get().clientFace.getPathNavNextPoint();
            if (nextPos == null || !nextPos.dimension().equals(level.dimension())) return;

            double playerX = Mth.lerp(event.getPartialTick(), player.xOld, player.getX());
            double playerY = Mth.lerp(event.getPartialTick(), player.yOld, player.getY());
            double playerZ = Mth.lerp(event.getPartialTick(), player.zOld, player.getZ());

            var direction = nextPos.pos().getCenter().subtract(playerX, playerY, playerZ);

            var poseStack = event.getPoseStack();
            poseStack.pushPose();

            var camPos = event.getCamera().getPosition().toVector3f();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
            poseStack.translate(playerX, playerY, playerZ);

            // rotate around player to point horizontally to the path point
            poseStack.mulPose(Axis.YP.rotation(-(float) Mth.atan2(direction.z, direction.x)));

            // adjust direction to make it point towards positive x-axis
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            // point vertically to the path point
            poseStack.mulPose(Axis.XP.rotation(-(float) Mth.atan2(direction.y, direction.horizontalDistance())));
            // adjust position to make it point towards positive x-axis
            poseStack.translate(0.25F, 0.75, 1.5F);

            var pose = poseStack.last().pose();
            var normal = poseStack.last().normal();

            var vc = NAV_ARROW_TEXTURE.buffer(mc.renderBuffers().bufferSource(), ModRenderType.CHECK_POINT);
            vertex(vc, pose, normal, 0, 0, 0, 1);
            vertex(vc, pose, normal, 1, 0, 1, 1);
            vertex(vc, pose, normal, 1, 1, 1, 0);
            vertex(vc, pose, normal, 0, 1, 0, 0);

            poseStack.popPose();
        }
    }

    /**
     * @see net.minecraft.client.renderer.entity.DragonFireballRenderer#vertex
     */
    @SuppressWarnings("JavadocReference")
    private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, int x, int z, int u,
                               int v) {
        vertexConsumer.vertex(pose, x / 2F - .5F, 0, z - .5F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }
}
