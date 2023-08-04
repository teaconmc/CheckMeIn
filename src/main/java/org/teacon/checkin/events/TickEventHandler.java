package org.teacon.checkin.events;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
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
    public static void updatePathPlannerGuide(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player && event.player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())
                && player.level().getGameTime() % 10 == 0) {

            var compoundTag = event.player.getMainHandItem().getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY);
            var teamID = compoundTag.getString(PathPlanner.TEAM_ID_KEY);
            var pathID = compoundTag.getString(PathPlanner.PATH_ID_KEY);

            CheckInPoints.of(event.player.level()).ifPresent(cap -> {
                var points = cap.nonnullOrdPathPoints(teamID, pathID)
                        .sorted(Comparator.comparing(data -> Objects.requireNonNull(data.ord())))
                        .map(PathPointData::pos)
                        .toList();
                var focus = compoundTag.contains(PathPlanner.LAST_POS_KEY, CompoundTag.TAG_COMPOUND)
                        ? NbtUtils.readBlockPos(compoundTag.getCompound(PathPlanner.LAST_POS_KEY))
                        : null;
                GuidingManager.of(event.player).ifPresent(gmCap -> {
                    if (!gmCap.getPathPlannerPoints().equals(points) || !Objects.equals(focus, gmCap.getPathPlannerFocus())) {
                        gmCap.setPathPlannerPoints(points);
                        CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PathPlannerGuidePacket(focus, points));
                    }
                });
            });
        }
    }
}
