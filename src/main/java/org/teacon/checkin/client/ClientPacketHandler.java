package org.teacon.checkin.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.client.gui.screens.inventory.PointPathScreen;
import org.teacon.checkin.client.gui.screens.inventory.PointUniqueScreen;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;
import org.teacon.checkin.network.protocol.game.PointPathScreenDataPacket;
import org.teacon.checkin.network.protocol.game.PointUniqueScreenDataPacket;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

public class ClientPacketHandler {
    public static void handlePathPlannerGuidePacket(PathPlannerGuidePacket packet, NetworkEvent.Context context) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            GuidingManager.of(mc.player).ifPresent(cap -> {
                cap.setPathPlannerFocus(packet.getFocus());
                cap.setPathPlannerPoints(packet.getGuidePoints());
            });
        }
    }

    public static void handlePointUniqueScreenDataPacket(PointUniqueScreenDataPacket packet, NetworkEvent.Context context) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(packet.getBlockPos()) instanceof PointUniqueBlockEntity
                && mc.screen instanceof PointUniqueScreen screen) {
            screen.updateGui(packet.getTeamID(), packet.getPointName());
        }
    }

    public static void handlePointPathScreenDataPacket(PointPathScreenDataPacket packet, NetworkEvent.Context context) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(packet.getBlockPos()) instanceof PointPathBlockEntity
                && mc.screen instanceof PointPathScreen screen) {
            screen.updateGui(packet.getTeamID(), packet.getPointName(), packet.getPathID(), packet.getOrd());
        }
    }
}
