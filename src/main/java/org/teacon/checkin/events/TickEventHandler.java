package org.teacon.checkin.events;


import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import org.apache.commons.lang3.tuple.Pair;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.configs.CommonConfig;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;
import org.teacon.checkin.network.protocol.game.SyncNextNavPointPacket;
import org.teacon.checkin.utils.MathHelper;
import org.teacon.checkin.world.item.PathPlanner;

import java.util.ArrayList;
import java.util.Comparator;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEventHandler {
    /**
     * This part can be computational-heavy.
     * Be sure to cache a few things including capabilities and avoid unnecessary operations.
     * <p>
     * Take care reading and resetting flag variables.
     */
    @SubscribeEvent
    public static void handleLevelTickEvent(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && !event.level.isClientSide) {
            var level = (ServerLevel) event.level;

            if (event.phase == TickEvent.Phase.END) {
                var points = CheckInPoints.of(level).resolve().orElse(null);

                var allPoints = new ArrayList<Pair<ServerLevel, CheckInPoints>>();
                for (var lvl : level.getServer().getAllLevels()) allPoints.add(Pair.of(lvl, CheckInPoints.of(lvl).resolve().orElse(null)));

                for (var player : level.players()) {
                    var guiding = GuidingManager.of(player).resolve().map($ -> $.serverFace).orElse(null);
                    if (points != null && guiding != null) {
                        syncPathPlannerGuidingPoints(player, level, points, guiding);

                        var progress = CheckProgress.of(player).resolve().orElse(null);
                        if (progress != null) {
                            syncPathNavGuidingPoints(player, level, points, guiding, progress, allPoints);
                            checkNearbyPathPoint(player, level, progress, allPoints);
                        }
                    }

                    if (guiding != null) guiding.setDimension(level.dimension());
                }
                if (points != null) points.pathsNeedSync().clear();
            }
        }
    }

    private static void syncPathPlannerGuidingPoints(ServerPlayer player, ServerLevel level, CheckInPoints points, GuidingManager.ServerFace guiding) {
        var mainHandItem = player.getMainHandItem();
        if (!mainHandItem.is(CheckMeIn.PATH_PLANNER.get())) {
            // should be fine to exit w/o syncing
            // cuz it's still tracked and client won't see it anyway
            guiding.setPathPlannerFocusID(null);
            return;
        }

        var compoundTag = mainHandItem.getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY);
        var id = new PathPointData.TeamPathID(
                compoundTag.getString(PathPlanner.TEAM_ID_KEY),
                compoundTag.getString(PathPlanner.PATH_ID_KEY));

        if (points.pathsNeedSync().contains(id) || !id.equals(guiding.getPathPlannerFocusID()) || !level.dimension().equals(guiding.getDimension())) {
            //noinspection DataFlowIssue
            var path = points.nonnullOrdPathPoints(id).stream()
                    .sorted(Comparator.comparing(PathPointData::ord)) // ord is non-null
                    .map(PathPointData::pos)
                    .toList();
            new PathPlannerGuidePacket(path).send(player);

            guiding.setPathPlannerFocusID(id);
        }
    }

    private static void syncPathNavGuidingPoints(ServerPlayer player, ServerLevel level, CheckInPoints points, GuidingManager.ServerFace guiding, CheckProgress progress,
            Iterable<Pair<ServerLevel, CheckInPoints>> allPoints) {
        var id = progress.getCurrentlyGuiding();
        if (progress.isNeedSyncPathProgress() || points.pathsNeedSync().contains(id) || !level.dimension().equals(guiding.getDimension())) {
            GlobalPos nextGlobal = null;
            if (id != null) {
                var nextPoint = nextPointOnPath(progress, allPoints, id);
                if (nextPoint != null) {
                    nextGlobal = GlobalPos.of(nextPoint.getLeft().dimension(), nextPoint.getRight().pos());
                }
            } 
            CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncNextNavPointPacket(nextGlobal));

            progress.setNeedSyncPathProgress(false);
        }
    }

    /**
     * Note: when points are added/changed into ord no greater than the last checked ord,
     * then their changes are ignored (player does not need to (re)visit them)
     */
    private static void checkNearbyPathPoint(ServerPlayer player, ServerLevel level, CheckProgress progress, Iterable<Pair<ServerLevel, CheckInPoints>> allPoints) {
        var id = progress.getCurrentlyGuiding();
        if (id == null) return;

        var nextOnPath = nextPointOnPath(progress, allPoints, id);
        if (nextOnPath != null) {
            var nextLevel = nextOnPath.getLeft();
            var nextPoint = nextOnPath.getRight();

            // assume the capabilities are synced, so no check for the actual block / block entity in the world
            if (nextLevel == level && MathHelper.chebyshevDist(nextPoint.pos(), player.blockPosition()) <= CommonConfig.INSTANCE.pathPointCheckInRange.get()) {
                player.playNotifySound(CheckMeIn.CHECK_PATH.get(), SoundSource.PLAYERS, 0.25F, 0.9F);
                assert nextPoint.ord() != null;
                progress.checkPathPoint(nextPoint.teamPathID(), nextPoint.ord());
            }
        }
    }

    @Nullable
    public static Pair<ServerLevel, PathPointData> nextPointOnPath(CheckProgress progress, Iterable<Pair<ServerLevel, CheckInPoints>> allPoints, PathPointData.TeamPathID id) {
        var lastOrd = progress.lastCheckedOrd(id);
        if (lastOrd == null) lastOrd = -1;

        ServerLevel level = null;
        PathPointData next = null;
        for (var pair : allPoints) {
            var lvl = pair.getLeft(); var points = pair.getRight();
            if (points == null) continue;

            var tmp = points.getNextPathPoint(id, lastOrd);
            assert tmp == null || tmp.ord() != null;
            if (tmp != null && (next == null || next.ord() > tmp.ord())) {
                next = tmp;
                level = lvl;
            }
        }
        return next == null ? null : Pair.of(level, next);
    }
}
