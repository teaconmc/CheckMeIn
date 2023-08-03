package org.teacon.checkin.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.teacon.checkin.client.renderer.ModRenderType;
import org.teacon.checkin.configs.ClientConfig;
import org.teacon.checkin.world.level.block.AbstractCheckPointBlock;


public class CheckPointBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final ResourceLocation texture;
    private final Minecraft mc;

    public CheckPointBlockRenderer(ResourceLocation texture) {
        this.texture = texture;
        this.mc = Minecraft.getInstance();
    }

    @Override
    public int getViewDistance() {
        return ClientConfig.INSTANCE.checkPointRenderDistance.get();
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, int packedOverlay) {
        var block = blockEntity.getBlockState().getBlock();
        var gm = this.mc.gameMode;
        var player = this.mc.player;
        if (gm == null || gm.getPlayerMode() != GameType.CREATIVE
                || player == null || !(block instanceof AbstractCheckPointBlock checkPointBlock)
                || !checkPointBlock.getRevealingItems().contains(player.getMainHandItem().getItem())) return;

        var gt = player.level().getGameTime();
        var rnd = blockEntity.getBlockPos().hashCode();
        var yOff = Math.sin((rnd + partialTick + gt) / 8) * 0.05;

        poseStack.pushPose();

        poseStack.translate(.5, .5 + yOff, .5);
        poseStack.mulPose(Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180));

        var lastPoseStack = poseStack.last();
        var pose = lastPoseStack.pose();
        var normal = lastPoseStack.normal();

//        FIXME: this "standard" way of applying outline does not work (see
//        var vertexConsumer = this.mc.renderBuffers().outlineBufferSource().getBuffer(RenderType.entityCutoutNoCull(texture));
        var vertexConsumer = multiBufferSource.getBuffer(ModRenderType.CHECK_POINT.apply(texture));
        vertex(vertexConsumer, pose, normal, 0, 0, 0, 1);
        vertex(vertexConsumer, pose, normal, 1, 0, 1, 1);
        vertex(vertexConsumer, pose, normal, 1, 1, 1, 0);
        vertex(vertexConsumer, pose, normal, 0, 1, 0, 0);

        poseStack.popPose();
    }

    /**
     * @see net.minecraft.client.renderer.entity.DragonFireballRenderer#vertex
     */
    @SuppressWarnings("JavadocReference")
    private static void vertex(VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normal, int x, int y, int u, int v) {
        vertexConsumer.vertex(pose, x - .5F, y - .5F, 0)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

}
