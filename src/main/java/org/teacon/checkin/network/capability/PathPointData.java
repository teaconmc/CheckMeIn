package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public record PathPointData(BlockPos pos, String teamID, String pointName, String pathID, int ord) {
    private static final String POS_KEY = "Pos";
    private static final String TEAM_ID_KEY = "TeamID";
    private static final String POINT_NAME_KEY = "PointName";
    private static final String PATH_ID_KEY = "PathID";
    @Nullable
    private static final String ORD_KEY = "Ord";

    public CompoundTag writeNBT() {
        var tag = new CompoundTag();
        tag.put(POS_KEY, NbtUtils.writeBlockPos(this.pos));
        tag.putString(TEAM_ID_KEY, this.teamID);
        tag.putString(POINT_NAME_KEY, this.pointName);
        tag.putString(PATH_ID_KEY, this.pathID);
        tag.putInt(ORD_KEY, this.ord);
        return tag;
    }

    public static Optional<PathPointData> readNBT(CompoundTag tag) {
        if (tag.contains(POS_KEY, CompoundTag.TAG_COMPOUND)
                && tag.contains(TEAM_ID_KEY, CompoundTag.TAG_STRING) && tag.contains(POINT_NAME_KEY, CompoundTag.TAG_STRING)
                && tag.contains(PATH_ID_KEY, CompoundTag.TAG_STRING) && tag.contains(ORD_KEY, CompoundTag.TAG_INT)) {
            return Optional.of(new PathPointData(NbtUtils.readBlockPos(tag.getCompound(POS_KEY)),
                    tag.getString(TEAM_ID_KEY), tag.getString(POINT_NAME_KEY), tag.getString(PATH_ID_KEY), tag.getInt(ORD_KEY)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPointData that = (PathPointData) o;
        return ord == that.ord && Objects.equals(pos, that.pos) && Objects.equals(teamID, that.teamID) && Objects.equals(pointName, that.pointName) && Objects.equals(pathID, that.pathID);
    }

    @Override
    public int hashCode() {return Objects.hash(pos, teamID, pointName, pathID, ord);}

    @Override
    public String toString() {
        return "PathPointData{" + "pos=" + pos + ", teamID='" + teamID + '\'' + ", pointName='" + pointName + '\'' +
                ", pathID='" + pathID + '\'' + ", ord=" + ord + '}';
    }
}
