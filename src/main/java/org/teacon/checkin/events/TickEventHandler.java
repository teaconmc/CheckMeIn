package org.teacon.checkin.events;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;
import org.teacon.checkin.world.item.PathPlanner;

import java.util.Comparator;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEventHandler {
    @SubscribeEvent
    public static void handleLevelTickEvent(TickEvent.LevelTickEvent event) {
        var level = event.level;
        if (event.phase == TickEvent.Phase.END) {
            for (var player : level.players()) syncPathPlannerGuidingPoints(player);

            CheckInPoints.of(level).ifPresent(points -> points.pathsNeedSync().clear());
        }
    }

    private static void syncPathPlannerGuidingPoints(Player player) {
        if (player instanceof ServerPlayer serverPlayer && player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())) {
            var compoundTag = player.getMainHandItem().getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY);
            var teamID = compoundTag.getString(PathPlanner.TEAM_ID_KEY);
            var pathID = compoundTag.getString(PathPlanner.PATH_ID_KEY);
            var id = new PathPointData.TeamPathID(teamID, pathID);

            CheckInPoints.of(player.level()).ifPresent(points -> GuidingManager.of(player).ifPresent(guiding -> {
                if (points.pathsNeedSync().contains(id) || !id.equals(guiding.serverFace.getPathPlannerFocusID())) {
                    var path = points.nonnullOrdPathPoints(teamID, pathID).stream()
                            .sorted(Comparator.comparing(data -> Objects.requireNonNull(data.ord())))
                            .map(PathPointData::pos)
                            .toList();
                    guiding.serverFace.setPathPlannerFocusID(id);
                    new PathPlannerGuidePacket(path).send(serverPlayer);
                }
            }));
        }
    }
}
