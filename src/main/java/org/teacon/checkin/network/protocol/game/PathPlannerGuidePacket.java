package org.teacon.checkin.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.client.ClientPacketHandler;

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
        context.enqueueWork(() -> ClientPacketHandler.handlePathPlannerGuidePacket(this, context));
        context.setPacketHandled(true);
    }

    @Nullable
    public BlockPos getFocus() {return focus;}

    public List<BlockPos> getGuidePoints() {return guidePoints;}
}
