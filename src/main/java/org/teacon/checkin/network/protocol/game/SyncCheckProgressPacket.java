package org.teacon.checkin.network.protocol.game;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;

import java.util.function.Supplier;

public class SyncCheckProgressPacket {

    PathPointData nextPointData;

    public SyncCheckProgressPacket(CheckProgress progress) {
        this.nextPointData = progress.getNextPoint();
    }

    public SyncCheckProgressPacket(FriendlyByteBuf buf) {
        this.nextPointData = buf.readNullable(PathPointData::readFromBuf);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNullable(this.nextPointData, PathPointData::writeToBuf1);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var ctx = contextSupplier.get();
        ctx.enqueueWork(() -> Handler.handle0(this, ctx));
        ctx.setPacketHandled(true);
    }

    private static final class Handler {
        public static void handle0(SyncCheckProgressPacket packet, NetworkEvent.Context context) {
            var p = Minecraft.getInstance().player;
            if (p == null) {
                return;
            }

            var progressData = p.getCapability(CheckProgress.Provider.CAPABILITY).resolve();
            if (progressData.isPresent()) {
                progressData.get().updateNextPoint(packet.nextPointData);
            }
        }
    }
}
