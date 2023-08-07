package org.teacon.checkin.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.function.Supplier;

public class PathNaviErasePacket {

    private final String teamID;
    private final String pathID;

    public PathNaviErasePacket(String teamID, String pathID) {
        this.teamID = teamID;
        this.pathID = pathID;
    }

    public PathNaviErasePacket(FriendlyByteBuf buf) {
        this.teamID = buf.readUtf();
        this.pathID = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(teamID);
        buf.writeUtf(pathID);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null && sender.containerMenu instanceof PathNavigatorMenu menu) {
                CheckProgress.of(sender).ifPresent(progress -> progress.resetPath(teamID, pathID));
                var cg = menu.getCurrentlyGuiding();
                if (cg != null && cg.teamId().equals(teamID) && cg.pathId().equals(pathID)) {
                    menu.updateCurrentlyGuidingUnsafe(new PathNavigatorMenu.Entry(cg.teamId(), cg.pathId(), cg.pointName(), cg.distance(), 0));
                }
            }
        });
        context.setPacketHandled(true);
    }

    public void send() {CheckMeIn.CHANNEL.sendToServer(this);}
}
