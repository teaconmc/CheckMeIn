package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collector;

@AutoRegisterCapability
public class CheckInPoints {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "points");

    private static final String UNIQUE_POINTS_KEY = "UniquePoints";
    private final Map<BlockPos, UniquePointData> blockPosUniquePointMap = new HashMap<>();
    private final Map<String, UniquePointData> teamIDUniquePointMap = new HashMap<>();

    private static final String PATH_POINTS_KEY = "PathPoints";
    private final Map<BlockPos, PathPointData> blockPosPathPointMap = new HashMap<>();

    private final Map<String, Map<String, Map<Integer, Collection<PathPointData>>>> teamPathIDOrdPointMap = new HashMap<>();
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
    public PathPointData getPathPoint(String teamID, String pathID, int ord) {
        var pathIDOrdMap = this.teamPathIDOrdPointMap.get(teamID);
        if (pathIDOrdMap == null) return null;
        var ordMap = pathIDOrdMap.get(pathID);
        if (ordMap == null) return null;
        var points = ordMap.get(ord);
        if (points == null) return null;
        for (var p : points) if (p.ord() != null && ord == p.ord()) return p;
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addPathPointIfAbsent(PathPointData pointData) {
        if (this.blockPosPathPointMap.putIfAbsent(pointData.pos(), pointData) == null) {
            var ordMap = this.teamPathIDOrdPointMap.computeIfAbsent(pointData.teamID(), k -> new HashMap<>())
                    .computeIfAbsent(pointData.pathID(), k -> new HashMap<>());
            if (pointData.ord() == null) ordMap.computeIfAbsent(null, k -> new ArrayList<>()).add(pointData);
            else ordMap.computeIfAbsent(pointData.ord(), k -> List.of(pointData));
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removePathPoint(BlockPos pos) {
        var data = blockPosPathPointMap.remove(pos);
        if (data == null) return false;

        var ordMap = teamPathIDOrdPointMap.get(data.teamID()).get(data.pathID());
        if (data.ord() == null) ordMap.get(null).removeIf(p -> data.pos().equals(p.pos()));
        else ordMap.remove(data.ord());
        return true;
    }

    public Collection<PathPointData> getAllPathPoints() {return this.blockPosPathPointMap.values();}

    /*                  Persistence                 */
    public void write(CompoundTag tag) {
        tag.put(UNIQUE_POINTS_KEY, this.getAllUniquePoints().stream().map(UniquePointData::writeNBT).collect(toListTag()));
        tag.put(PATH_POINTS_KEY, this.getAllPathPoints().stream().map(PathPointData::writeNBT).collect(toListTag()));
    }

    public void read(CompoundTag tag) {
        if (tag.contains(UNIQUE_POINTS_KEY, CompoundTag.TAG_LIST)) {
            this.blockPosUniquePointMap.clear();
            this.teamIDUniquePointMap.clear();
            for (var t : tag.getList(UNIQUE_POINTS_KEY, CompoundTag.TAG_COMPOUND))
                if (t instanceof CompoundTag ct) UniquePointData.readNBT(ct).ifPresent(this::addUniquePointIfAbsent);
        }
        if (tag.contains(PATH_POINTS_KEY, CompoundTag.TAG_LIST)) {
            this.blockPosPathPointMap.clear();
            this.teamPathIDOrdPointMap.clear();
            for (var t : tag.getList(PATH_POINTS_KEY, CompoundTag.TAG_COMPOUND))
                if (t instanceof CompoundTag ct) PathPointData.readNBT(ct).ifPresent(this::addPathPointIfAbsent);
        }
    }

    private static Collector<CompoundTag, ListTag, ListTag> toListTag() {
        return Collector.of(ListTag::new, ListTag::add, (l1, l2) -> {
            l1.add(l2);
            return l1;
        });
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
}
