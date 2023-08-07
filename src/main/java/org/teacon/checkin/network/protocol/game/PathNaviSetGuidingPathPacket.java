package org.teacon.checkin.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.function.Supplier;

public class PathNaviSetGuidingPathPacket {

    private final String teamId, pathId;

    public PathNaviSetGuidingPathPacket(String teamId, String pathId) {
        this.teamId = teamId;
        this.pathId = pathId;
    }

    public PathNaviSetGuidingPathPacket() {
        this.teamId = null;
        this.pathId = null;
    }

    public PathNaviSetGuidingPathPacket(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            this.teamId = buf.readUtf();
            this.pathId = buf.readUtf();
        } else {
            this.teamId = null;
            this.pathId = null;
        }
    }

    public void write(FriendlyByteBuf buf) {
        if (this.teamId == null || this.pathId == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeUtf(this.teamId);
            buf.writeUtf(this.pathId);
        }
    }


    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null && sender.containerMenu instanceof PathNavigatorMenu menu) {
                var guiding = new PathPointData.TeamPathID(this.teamId, this.pathId);
                CheckProgress.of(sender).ifPresent(progress -> progress.setCurrentlyGuiding(guiding));
                menu.updateCurrentlyGuiding(this.teamId, this.pathId);
            }
        });
        context.setPacketHandled(true);
    }
}
