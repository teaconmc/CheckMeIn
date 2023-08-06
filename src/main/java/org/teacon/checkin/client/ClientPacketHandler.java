package org.teacon.checkin.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;

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
}
