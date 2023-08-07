package org.teacon.checkin.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

import java.util.Objects;
import java.util.function.Supplier;

public class PathNaviTpBackPacket {
    private final String teamID;
    private final String pathID;

    public PathNaviTpBackPacket(String teamID, String pathID) {
        this.teamID = teamID;
        this.pathID = pathID;
    }

    public PathNaviTpBackPacket(FriendlyByteBuf buf) {
        this.teamID = buf.readUtf();
        this.pathID = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(teamID);
        buf.writeUtf(pathID);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> this.doHandle(context));
        context.setPacketHandled(true);
    }

    private void doHandle(NetworkEvent.Context context) {
        var sender = context.getSender();
        if (sender == null || !(sender.containerMenu instanceof PathNavigatorMenu)) return;
        sender.closeContainer();

        var progOpt = CheckProgress.of(sender).resolve();
        if (progOpt.isEmpty()) return;

        var lastOrd = progOpt.get().lastCheckedOrd(teamID, pathID);
        if (lastOrd == null) {
            sender.sendSystemMessage(Component.translatable("item.check_in.nvg_path.path_not_start"));
            return;
        }

        PathPointData last = null;
        ServerLevel lastLvl = null;
        for (var lvl : sender.serverLevel().getServer().getAllLevels()) {
            var pointsOpt = CheckInPoints.of(lvl).resolve();
            if (pointsOpt.isEmpty()) continue;

            var tmp = pointsOpt.get().getPathPoint(teamID, pathID, lastOrd);
            if (tmp != null) {
                last = tmp;
                lastLvl = lvl;
                break;
            }
        }

        // the ord in record may not exist when the path is modified after check-in
        if (last == null) {
            for (var lvl : sender.serverLevel().getServer().getAllLevels()) {
                var pointsOpt = CheckInPoints.of(lvl).resolve();
                if (pointsOpt.isEmpty()) continue;

                for (var data : pointsOpt.get().nonnullOrdPathPoints(teamID, pathID)) {
                    if (data.ord() != null && data.ord() < lastOrd
                            && (last == null || data.ord() < Objects.requireNonNull(last.ord()))) {
                        last = data;
                        lastLvl = lvl;
                    }
                }
            }
            if (last == null) {
                CheckMeIn.LOGGER.warn("Player " + sender.getDisplayName() + " has progress " + lastOrd + " in path " + pathID + " of team " + teamID + ", but the");
                return;
            }
        }

        var pos = Vec3.atBottomCenterOf(last.pos());
        sender.teleportTo(lastLvl, pos.x, pos.y, pos.z, sender.getYRot(), sender.getXRot());
    }

    public void send() {CheckMeIn.CHANNEL.sendToServer(this);}
}
