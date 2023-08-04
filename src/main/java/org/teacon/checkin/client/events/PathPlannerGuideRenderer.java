package org.teacon.checkin.client.events;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.client.renderer.ModRenderType;
import org.teacon.checkin.configs.ClientConfig;
import org.teacon.checkin.network.capability.GuidingManager;

import java.awt.*;
import java.util.OptionalDouble;

@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PathPlannerGuideRenderer {
    private static final int START_COLOR = new Color(0xB8473D).getRGB();
    private static final int END_COLOR = new Color(0x3DB884).getRGB();

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var player = mc.player;
        if (!player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())) return;

        var capOpt = GuidingManager.of(mc.player).resolve();
        if (capOpt.isPresent()) {
            var pathPlannerPoints = capOpt.get().getPathPlannerPoints();
            var focus = capOpt.get().getPathPlannerFocus();
            var playerPos = player.position();

            var poseStack = event.getPoseStack();
            poseStack.pushPose();

            var camPos = event.getCamera().getPosition().toVector3f();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            var pose = poseStack.last().pose();
            var normal = poseStack.last().normal();

            final int viewDist = ClientConfig.INSTANCE.checkPointRenderDistance.get();

            // highlight focusing point
            if (focus != null && Vec3.atCenterOf(focus).closerThan(playerPos, viewDist)) {
                var vc = mc.renderBuffers().bufferSource().getBuffer(ModRenderType.GUIDE_LINE.apply(OptionalDouble.of(3)));
                var center = focus.getCenter().subtract(0, 0.5, 0).toVector3f();
                var r = 0.25f;
                drawLine(vc, pose, normal, new Vector3f(center).sub(-r, 0, 0), new Vector3f(center).sub(0, 0, -r), 0xFFFFFFFF, 0xFFFFFFFF);
                drawLine(vc, pose, normal, new Vector3f(center).sub(0, 0, -r), new Vector3f(center).sub(r, 0, 0), 0xFFFFFFFF, 0xFFFFFFFF);
                drawLine(vc, pose, normal, new Vector3f(center).sub(r, 0, 0), new Vector3f(center).sub(0, 0, r), 0xFFFFFFFF, 0xFFFFFFFF);
                drawLine(vc, pose, normal, new Vector3f(center).sub(0, 0, r), new Vector3f(center).sub(-r, 0, 0), 0xFFFFFFFF, 0xFFFFFFFF);
            }

            var dTotal = 0f;
            // draw stroke
            var vc = mc.renderBuffers().bufferSource().getBuffer(ModRenderType.GUIDE_LINE.apply(OptionalDouble.of(7)));
            for (int i = 1; i < pathPlannerPoints.size(); i++) {
                if (!Vec3.atCenterOf(pathPlannerPoints.get(i - 1)).closerThan(playerPos, viewDist)
                        && !Vec3.atCenterOf(pathPlannerPoints.get(i)).closerThan(playerPos, viewDist))
                    continue;
                var p1 = pathPlannerPoints.get(i - 1).getCenter().subtract(0, 0.5, 0).toVector3f();
                var p2 = pathPlannerPoints.get(i).getCenter().subtract(0, 0.5, 0).toVector3f();
                drawLine(vc, pose, normal, p1, p2, 0x7FFFFFFF, 0x7FFFFFFF);

                dTotal += p1.distance(p2);
            }

            // draw line
            vc = mc.renderBuffers().bufferSource().getBuffer(ModRenderType.GUIDE_LINE.apply(OptionalDouble.of(5)));
            var d = 0f;
            for (int i = 1; i < pathPlannerPoints.size(); i++) {
                if (!Vec3.atCenterOf(pathPlannerPoints.get(i - 1)).closerThan(playerPos, viewDist)
                        && !Vec3.atCenterOf(pathPlannerPoints.get(i)).closerThan(playerPos, viewDist))
                    continue;
                var p1 = pathPlannerPoints.get(i - 1).getCenter().subtract(0, 0.5, 0).toVector3f();
                var p2 = pathPlannerPoints.get(i).getCenter().subtract(0, 0.5, 0).toVector3f();
                var dTmp = d + p2.distance(p1);
                drawLine(vc, pose, normal, p1, p2, ARGB32.lerp(d / dTotal, START_COLOR, END_COLOR),
                        ARGB32.lerp(dTmp / dTotal, START_COLOR, END_COLOR));
                d = dTmp;
            }

            poseStack.popPose();
        }
    }

    public static void drawLine(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vector3f p1, Vector3f p2, int argb1, int argb2) {
        var n = new Vector3f(p2).sub(p1);
        vc.vertex(pose, p1.x, p1.y, p1.z).color(ARGB32.red(argb1), ARGB32.green(argb1), ARGB32.blue(argb1), ARGB32.alpha(argb1)).normal(normal, n.x, n.y, n.z).endVertex();
        vc.vertex(pose, p2.x, p2.y, p2.z).color(ARGB32.red(argb2), ARGB32.green(argb2), ARGB32.blue(argb2), ARGB32.alpha(argb2)).normal(normal, n.x, n.y, n.z).endVertex();
    }
}
