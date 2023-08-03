package org.teacon.checkin.network.protocol.game;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.GuidingManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PathPlannerGuidePacket {
    @Nullable
    private final BlockPos focus;
    private final List<BlockPos> guidePoints;

    public PathPlannerGuidePacket(@Nullable BlockPos focus, List<BlockPos> guidePoints) {
        this.focus = focus;
        this.guidePoints = guidePoints;
    }

    public PathPlannerGuidePacket(FriendlyByteBuf buf) {
        this.focus = !buf.readBoolean() ? buf.readBlockPos() : null;

        var n = buf.readInt();
        this.guidePoints = new ArrayList<>(n);
        while (n-- > 0) this.guidePoints.add(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.focus == null);
        if (this.focus != null) buf.writeBlockPos(this.focus);

        buf.writeInt(this.guidePoints.size());
        guidePoints.forEach(buf::writeBlockPos);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> this.doHandle(context));
        context.setPacketHandled(true);
    }

    public void doHandle(NetworkEvent.Context context) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        GuidingManager.of(mc.player).ifPresent(cap -> {
            cap.setPathPlannerFocus(this.focus);
            cap.setPathPlannerPoints(ImmutableList.copyOf(this.guidePoints));
        });
    }
}
