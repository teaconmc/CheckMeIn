package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.utils.NbtHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@AutoRegisterCapability
public class CheckInPoints {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "points");

    private static final String UNIQUE_POINTS_KEY = "UniquePoints";
    private final Map<BlockPos, UniquePointData> blockPosUniquePointMap = new HashMap<>();
    private final Map<String, UniquePointData> teamIDUniquePointMap = new HashMap<>();

    private static final String PATH_POINTS_KEY = "PathPoints";
    private final Map<BlockPos, PathPointData> blockPosPathPointMap = new HashMap<>();
    private final Map<PathPointData.TeamPathID, NullableOrdMap> teamPathIDPathPointMap = new HashMap<>();
    private final Set<PathPointData.TeamPathID> pathsNeedSync = new HashSet<>();

    public CheckInPoints() {}

    /*                  Unique Points                   */
    @Nullable
    public UniquePointData getUniquePoint(BlockPos pos) {return this.blockPosUniquePointMap.get(pos);}

    @Nullable
    public UniquePointData getUniquePoint(String teamID) {return this.teamIDUniquePointMap.get(teamID);}

    @SuppressWarnings("UnusedReturnValue")
    public boolean addUniquePointIfAbsent(UniquePointData pointData) {
        if (blockPosUniquePointMap.putIfAbsent(pointData.pos(), pointData) == null) {
            teamIDUniquePointMap.put(pointData.teamID(), pointData);
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeUniquePoint(BlockPos pos) {
        var data = blockPosUniquePointMap.remove(pos);
        if (data == null) return false;
        teamIDUniquePointMap.remove(data.teamID());
        return true;
    }

    public Collection<UniquePointData> getAllUniquePoints() {return this.blockPosUniquePointMap.values();}

    /*                  Path Points                 */
    @Nullable
    public PathPointData getPathPoint(BlockPos pos) {return this.blockPosPathPointMap.get(pos);}

    @Nullable
    public PathPointData getPathPoint(String teamID, String pathID, short ord) {
        var ordMap = this.teamPathIDPathPointMap.get(new PathPointData.TeamPathID(teamID, pathID));
        return ordMap == null ? null : ordMap.get(ord);
    }

    public Collection<PathPointData> nonnullOrdPathPoints(String teamID, String pathID) {
        var ordMap = this.teamPathIDPathPointMap.get(new PathPointData.TeamPathID(teamID, pathID));
        return ordMap == null ? List.of() : ordMap.nonnullOrdValues();
    }

    public void addPathPointIfAbsent(PathPointData pointData) {
        if (this.blockPosPathPointMap.putIfAbsent(pointData.pos(), pointData) == null) {
            var id = pointData.teamPathID();
            this.teamPathIDPathPointMap.computeIfAbsent(id, k -> new NullableOrdMap()).addIfAbsent(pointData);
            this.pathsNeedSync.add(id);
        }
    }

    public void removePathPoint(BlockPos pos) {
        var data = blockPosPathPointMap.remove(pos);
        if (data != null) {
            var id = data.teamPathID();
            teamPathIDPathPointMap.get(id).remove(data);
            this.pathsNeedSync.add(id);
        }
    }

    public Collection<PathPointData> getAllPathPoints() {return this.blockPosPathPointMap.values();}

    public Collection<PathPointData.TeamPathID> getTeamPathIDs() {return this.teamPathIDPathPointMap.keySet();}

    public Collection<PathPointData.TeamPathID> pathsNeedSync() {return this.pathsNeedSync;}

    /*                  Persistence                 */
    public void write(CompoundTag tag) {
        tag.put(UNIQUE_POINTS_KEY, this.getAllUniquePoints().stream().map(UniquePointData::writeNBT).collect(NbtHelper.toListTag()));
        tag.put(PATH_POINTS_KEY, this.getAllPathPoints().stream().map(PathPointData::writeNBT).collect(NbtHelper.toListTag()));
    }

    public void read(CompoundTag tag) {
        if (tag.contains(UNIQUE_POINTS_KEY, CompoundTag.TAG_LIST)) {
            this.blockPosUniquePointMap.clear();
            this.teamIDUniquePointMap.clear();
            for (var t : tag.getList(UNIQUE_POINTS_KEY, CompoundTag.TAG_COMPOUND))
                UniquePointData.readNBT((CompoundTag) t).ifPresent(this::addUniquePointIfAbsent);
        }
        if (tag.contains(PATH_POINTS_KEY, CompoundTag.TAG_LIST)) {
            this.blockPosPathPointMap.clear();
            this.teamPathIDPathPointMap.clear();
            for (var t : tag.getList(PATH_POINTS_KEY, CompoundTag.TAG_COMPOUND))
                PathPointData.readNBT((CompoundTag) t).ifPresent(this::addPathPointIfAbsent);
        }
    }

    public static LazyOptional<CheckInPoints> of(Level level) {
        return level.getCapability(Provider.CAPABILITY);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        public static final Capability<CheckInPoints> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

        private final CheckInPoints checkInPoints = new CheckInPoints();
        private final LazyOptional<CheckInPoints> capability = LazyOptional.of(() -> checkInPoints);

        @Override
        public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.capability);
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            this.checkInPoints.write(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.checkInPoints.read(tag);
        }
    }

    public static class NullableOrdMap {
        private final Map<Short, PathPointData> ordMap;
//        private final Set<PathPointData> nullOrdMap;

        public NullableOrdMap() {
            this.ordMap = new HashMap<>();
//            this.nullOrdMap = new HashSet<>();
        }

        public void addIfAbsent(PathPointData data) {
            if (data.ord() != null) ordMap.putIfAbsent(data.ord(), data);
//            else nullOrdMap.add(data);
        }

        public void remove(PathPointData data) {
            if (data.ord() != null) ordMap.remove(data.ord());
//            else nullOrdMap.remove(data);
        }

        @Nullable
        public PathPointData get(short ord) {return ordMap.get(ord);}

        public Collection<PathPointData> nonnullOrdValues() {return ordMap.values();}
    }
}
