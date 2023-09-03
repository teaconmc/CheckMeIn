package org.teacon.checkin.events;


import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.configs.CommonConfig;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.CheckProgress;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.network.capability.PathPointData;
import org.teacon.checkin.network.protocol.game.PathPlannerGuidePacket;
import org.teacon.checkin.network.protocol.game.SyncCheckProgressPacket;
import org.teacon.checkin.utils.MathHelper;
import org.teacon.checkin.world.item.PathPlanner;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

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

            for (var player : level.players()) checkNearbyPathPoint(player);
        }
    }

    private static void syncPathPlannerGuidingPoints(Player player) {
        if (player instanceof ServerPlayer serverPlayer && player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())) {
            var compoundTag = player.getMainHandItem().getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY);
            var teamID = compoundTag.getString(PathPlanner.TEAM_ID_KEY);
            var pathID = compoundTag.getString(PathPlanner.PATH_ID_KEY);
            var id = new PathPointData.TeamPathID(teamID, pathID);
            var dim = player.level().dimension();

            CheckInPoints.of(player.level()).ifPresent(points -> GuidingManager.of(player).ifPresent(guiding -> {
                if (points.pathsNeedSync().contains(id) || !id.equals(guiding.serverFace.getPathPlannerFocusID()) || !dim.equals(guiding.serverFace.getDimension())) {
                    var path = points.nonnullOrdPathPoints(teamID, pathID).stream()
                            .sorted(Comparator.comparing(data -> Objects.requireNonNull(data.ord())))
                            .map(PathPointData::pos)
                            .toList();
                    guiding.serverFace.setPathPlannerFocusID(id);
                    guiding.serverFace.setDimension(dim);
                    new PathPlannerGuidePacket(path).send(serverPlayer);
                }
            }));
        }
    }

    /**
     * Note: when points are added/changed into ord no greater than the last checked ord,
     * then their changes are ignored (player does not need to (re)visit them)
     */
    private static void checkNearbyPathPoint(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var level = ((ServerPlayer) player).serverLevel();

        var progOpt = CheckProgress.of(serverPlayer).resolve();
        var pointsOpt = CheckInPoints.of(level).resolve();
        if (progOpt.isEmpty() || pointsOpt.isEmpty()) return;
        var prog = progOpt.get();
        var points = pointsOpt.get();

        var guidingPath = prog.getCurrentlyGuiding();
        if (guidingPath == null) return;

        var teamID = guidingPath.teamID();
        var pathID = guidingPath.pathID();
        var tmpOrd = progOpt.get().lastCheckedOrd(teamID, pathID);
        final short lastOrd = tmpOrd == null ? -1 : tmpOrd;

        var playerPos = serverPlayer.blockPosition();
        final int r = CommonConfig.INSTANCE.pathPointCheckInRange.get();
        ChunkPos cp1 = new ChunkPos(playerPos.offset(-r, -r, -r)), cp2 = new ChunkPos(playerPos.offset(r, r, r));
        ChunkPos.rangeClosed(cp1, cp2)
                .flatMap(pos -> level.getChunk(pos.x, pos.z).getBlockEntities().entrySet().stream())
                .filter(entry -> entry.getValue() instanceof PointPathBlockEntity)
                .map(entry -> points.getPathPoint(entry.getKey()))
                .filter(data -> data != null && MathHelper.chebyshevDist(data.pos(), playerPos) <= r
                        && data.teamID().equals(teamID) && data.pathID().equals(pathID)
                        && data.ord() != null && (lastOrd == -1 ? data.ord() == 0 || data.ord() == 1 : data.ord() == lastOrd + 1)) // TODO 允许不连续的路径点 ord。举例：只存在 1, 2, 4, 8，当已在 1 和 2 签到时，不能在 8 签到，必须先在 4 签到。
                .findFirst()
                .ifPresent(next -> {
                    player.playNotifySound(CheckMeIn.CHECK_PATH.get(), SoundSource.PLAYERS, 0.25F, 0.9F);
                    assert next.ord() != null;
                    prog.checkPathPoint(next.teamPathID(), next.ord());
                });

        var maybeLastPointOrd = prog.lastCheckedOrd(teamID, pathID);
        if (maybeLastPointOrd != null) {
            var nextPoint = points.getPathPoint(teamID, pathID, (short) (maybeLastPointOrd + 1));
            var prev = prog.updateNextPoint(nextPoint);
            if (prev != nextPoint) {
                CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncCheckProgressPacket(prog));
            }
        } else {
            var nextPoint = points.nonnullOrdPathPoints(teamID, pathID)
                    .stream()
                    .min(Comparator.comparing(p -> p.ord() == null ? -1 : p.ord()))
                    .orElse(null);
            if (nextPoint != null) {
                var prev = prog.updateNextPoint(nextPoint);
                if (prev != nextPoint) {
                    CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncCheckProgressPacket(prog));
                }
            }
        }

    }
}
