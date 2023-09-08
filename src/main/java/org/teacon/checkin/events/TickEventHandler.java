package org.teacon.checkin.events;


import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
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
import org.teacon.checkin.network.protocol.game.SyncNextNavPointPacket;
import org.teacon.checkin.utils.MathHelper;
import org.teacon.checkin.world.item.PathPlanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = CheckMeIn.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickEventHandler {
    @SubscribeEvent
    public static void handleLevelTickEvent(TickEvent.LevelTickEvent event) {
        var level = event.level;
        if (event.phase == TickEvent.Phase.END) {
            var resetFlagCallbacks = new ArrayList<Runnable>();
            for (var player : level.players()) {
                // flags should be reset in callback functions to prevent interfering following syncing functions
                // flags are reset after each tick, so everything sync-able should always be synced
                resetFlagCallbacks.add(syncPathPlannerGuidingPoints(player));
                resetFlagCallbacks.add(syncPathNavGuidingPoints(player));
            }
            resetFlagCallbacks.forEach(Runnable::run);

            for (var player : level.players()) checkNearbyPathPoint(player);
        }
    }

    private static Runnable syncPathPlannerGuidingPoints(Player player) {
        var callbackHolder = new Runnable[]{() -> {}};
        if (player instanceof ServerPlayer serverPlayer) {
            // when the item is not a PathPlanner, the ids will be empty strings, and a path of length 0 will be synced
            var compoundTag = player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())
                    ? player.getMainHandItem().getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY)
                    : new CompoundTag();
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
                    new PathPlannerGuidePacket(path).send(serverPlayer);
                    callbackHolder[0] = () -> {
                        guiding.serverFace.setPathPlannerFocusID(id);
                        guiding.serverFace.setDimension(dim);
                        points.pathsNeedSync().clear();
                    };
                }
            }));
        }
        return callbackHolder[0];
    }

    private static Runnable syncPathNavGuidingPoints(Player player) {
        var callbackHolder = new Runnable[]{() -> {}};
        if (player instanceof ServerPlayer serverPlayer) {
            CheckInPoints.of(serverPlayer.serverLevel()).ifPresent(points -> CheckProgress.of(serverPlayer).ifPresent(progress -> GuidingManager.of(serverPlayer).ifPresent(guiding -> {
                var id = progress.getCurrentlyGuiding();
                var dim = player.level().dimension();
                if (progress.isNeedSyncPathProgress() || points.pathsNeedSync().contains(id) || !dim.equals(guiding.serverFace.getDimension())) {
                    var nextGlobal = Optional.ofNullable(progress.getCurrentlyGuiding())
                            .map($ -> CheckProgress.nextPointOnPath(serverPlayer, $.teamID(), $.pathID()))
                            .map($ -> GlobalPos.of($.getLeft().dimension(), $.getRight().pos()))
                            .orElse(null);
                    CheckMeIn.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncNextNavPointPacket(nextGlobal));
                    callbackHolder[0] = () -> {
                        guiding.serverFace.setDimension(dim);
                        progress.setNeedSyncPathProgress(false);
                        points.pathsNeedSync().clear();
                    };
                }
            })));
        }
        return callbackHolder[0];
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

        var guidingPath = prog.getCurrentlyGuiding();
        if (guidingPath == null) return;

        var nextOnPath = CheckProgress.nextPointOnPath(serverPlayer, guidingPath.teamID(), guidingPath.pathID());
        if (nextOnPath != null) {
            var nextLevel = nextOnPath.getLeft();
            var nextPoint = nextOnPath.getRight();

            // assume the capabilities are synced, so no check for the actual block / block entity in the world
            if (nextLevel == level && MathHelper.chebyshevDist(nextPoint.pos(), serverPlayer.blockPosition()) <= CommonConfig.INSTANCE.pathPointCheckInRange.get()) {
                player.playNotifySound(CheckMeIn.CHECK_PATH.get(), SoundSource.PLAYERS, 0.25F, 0.9F);
                assert nextPoint.ord() != null;
                prog.checkPathPoint(nextPoint.teamPathID(), nextPoint.ord());
            }
        }
    }
}
