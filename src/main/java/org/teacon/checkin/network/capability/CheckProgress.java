package org.teacon.checkin.network.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.tuple.Pair;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.utils.NbtHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@AutoRegisterCapability
public class CheckProgress {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "progress");
    private static final String UNIQUE_POINTS_KEY = "UniquePoints";
    private static final String PATH_PROGRESS_KEY = "PathProgress";
    private static final String CURRENTLY_GUIDING_KEY = "CurrentlyGuiding";
    private static final String PROGRESS_KEY = "Progress";

    private final Map<String, UniquePointData> uniquePoints = new HashMap<>();

    private final Map<PathPointData.TeamPathID, Short> pathProgress = new HashMap<>();
    private @Nullable PathPointData.TeamPathID currentlyGuiding = null;
    /**
     * should be set to true whenever currentlyGuiding or pathProgress is updated
     */
    private boolean needSyncPathProgress = false;

    public void setCurrentlyGuiding(@Nullable PathPointData.TeamPathID currentlyGuiding) {
        this.currentlyGuiding = currentlyGuiding;
        this.needSyncPathProgress = true;
    }

    public @Nullable PathPointData.TeamPathID getCurrentlyGuiding() {
        return currentlyGuiding;
    }

    public boolean isUniquePointChecked(UniquePointData data) {return uniquePoints.containsKey(data.teamID());}

    /**
     * Check a unique point only when it is not previously checked
     *
     * @return if a new point is checked
     */
    public boolean checkUniquePoint(UniquePointData data) {return uniquePoints.putIfAbsent(data.teamID(), data) == null;}

    /**
     * Advance the check-in progress
     */
    public void checkPathPoint(PathPointData.TeamPathID id, short ord) {
        pathProgress.compute(id, (k, v) -> v == null || v < ord ? ord : v);
        this.needSyncPathProgress = true;
    }

    public void resetUniquePoint(String teamID) {uniquePoints.remove(teamID);}

    public void resetAllUniquePoints() {uniquePoints.clear();}

    public void resetPath(String teamID, String pathID) {
        pathProgress.remove(new PathPointData.TeamPathID(teamID, pathID));
        this.needSyncPathProgress = true;
    }

    public void resetAllPaths() {
        pathProgress.clear();
        this.needSyncPathProgress = true;
    }

    @Nullable
    public Short lastCheckedOrd(String teamID, String pathID) {return pathProgress.get(new PathPointData.TeamPathID(teamID, pathID));}

    public void copyFrom(CheckProgress progress) {
        this.uniquePoints.clear();
        this.pathProgress.clear();
        this.uniquePoints.putAll(progress.uniquePoints);
        this.pathProgress.putAll(progress.pathProgress);
        this.currentlyGuiding = progress.currentlyGuiding;
    }

    public Collection<String> checkedUniquePointTeamIDs() {return uniquePoints.keySet();}

    public boolean isNeedSyncPathProgress() {return needSyncPathProgress;}

    public void setNeedSyncPathProgress(boolean needSyncPathProgress) {this.needSyncPathProgress = needSyncPathProgress;}

    /*                  Persistence                 */
    public void write(CompoundTag tag) {
        tag.put(UNIQUE_POINTS_KEY, this.uniquePoints.values().stream().map(UniquePointData::writeNBT).collect(NbtHelper.toListTag()));
        tag.put(PATH_PROGRESS_KEY, this.pathProgress.entrySet().stream().map(entry -> {
            var compound = entry.getKey().writeNBT();
            compound.putShort(PROGRESS_KEY, entry.getValue());
            return compound;
        }).collect(NbtHelper.toListTag()));
        if (this.currentlyGuiding != null) {
            tag.put(CURRENTLY_GUIDING_KEY, this.currentlyGuiding.writeNBT());
        }
    }

    public void read(CompoundTag tag) {
        if (tag.contains(UNIQUE_POINTS_KEY, CompoundTag.TAG_LIST)) {
            this.uniquePoints.clear();
            for (var t : tag.getList(UNIQUE_POINTS_KEY, CompoundTag.TAG_COMPOUND))
                UniquePointData.readNBT((CompoundTag) t).ifPresent(this::checkUniquePoint);
        }
        if (tag.contains(PATH_PROGRESS_KEY, CompoundTag.TAG_LIST)) {
            this.pathProgress.clear();
            for (var t : tag.getList(PATH_PROGRESS_KEY, CompoundTag.TAG_COMPOUND)) {
                var ct = (CompoundTag) t;
                PathPointData.TeamPathID.readNBT(ct).ifPresent(id -> {
                    if (ct.contains(PROGRESS_KEY, CompoundTag.TAG_SHORT))
                        this.checkPathPoint(id, ct.getShort(PROGRESS_KEY));
                });
            }
        }
        if (tag.contains(CURRENTLY_GUIDING_KEY)) {
            this.currentlyGuiding = PathPointData.TeamPathID.readNBT(tag.getCompound(CURRENTLY_GUIDING_KEY)).orElse(null);
        }
    }

    /*                  Utilities                 */
    public static LazyOptional<CheckProgress> of(ServerPlayer player) {
        return player.getCapability(Provider.CAPABILITY);
    }

    @Nullable
    public static Pair<ServerLevel, PathPointData> nextPointOnPath(ServerPlayer player, String teamID, String pathID) {
        var progOpt = CheckProgress.of(player).resolve();
        if (progOpt.isEmpty()) return null;
        var lastOrd = progOpt.get().lastCheckedOrd(teamID, pathID);
        if (lastOrd == null) lastOrd = -1;

        ServerLevel level = null;
        PathPointData next = null;
        for (var lvl : player.server.getAllLevels()) {
            var pointsOpt = CheckInPoints.of(lvl).resolve();
            if (pointsOpt.isEmpty()) continue;

            var tmp = pointsOpt.get().getNextPathPoint(teamID, pathID, lastOrd);
            assert tmp == null || tmp.ord() != null;
            if (tmp != null && (next == null || next.ord() > tmp.ord())) {
                next = tmp;
                level = lvl;
            }
        }
        return next == null ? null : Pair.of(level, next);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        public static final Capability<CheckProgress> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

        private final CheckProgress checkProgress = new CheckProgress();
        private final LazyOptional<CheckProgress> capability = LazyOptional.of(() -> checkProgress);

        @Override
        public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.capability);
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            this.checkProgress.write(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.checkProgress.read(tag);
        }
    }
}
