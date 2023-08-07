package org.teacon.checkin.client;

import net.minecraft.client.Minecraft;
import org.teacon.checkin.client.gui.screens.inventory.PathNavigatorScreen;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.protocol.game.PathNaviPageResponsePacket;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;
import org.teacon.checkin.world.inventory.PathNavigatorMenu;

public class ClientPacketHandler {
    public static void handlePathPlannerGuidePacket(PathPlannerGuidePacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.player != null)
            GuidingManager.of(mc.player).ifPresent(cap -> cap.clientFace.setPathPlannerPoints(packet.getGuidePoints()));
    }

    public static void handlePathNaviPageResponsePacket(PathNaviPageResponsePacket pathNaviPageResponsePacket) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof PathNavigatorMenu menu) {
            menu.receivePage(pathNaviPageResponsePacket.getPageNo(), pathNaviPageResponsePacket.getEntries());
            if (mc.screen instanceof PathNavigatorScreen screen) screen.init();
        }
    }
}
