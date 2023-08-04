package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Defines the data of each PointPathBlock.
 * <p>
 * No two points should share the same combination of {@code teamID, pointName, pathID, ord}.
 * When {@code ord} does not exist, all fields but {@code pos} are duplicable.
 *
 * @param pos       BlockPos of PointPathBlock. Duplicable only if two blocks are in different worlds
 *                  (and stored in different capabilities)
 * @param teamID    Duplication allowed
 * @param pointName Duplication allowed
 * @param pathID    Duplication allowed
 * @param ord       The ordinal number starting from 0. This can be {@code null} for which
 *                  the point does not count toward the visiting progress
 */
public record PathPointData(BlockPos pos, String teamID, String pointName, String pathID, @Nullable Integer ord) {
    private static final String POS_KEY = "Pos";
    private static final String TEAM_ID_KEY = "TeamID";
    private static final String POINT_NAME_KEY = "PointName";
    private static final String PATH_ID_KEY = "PathID";
    private static final String ORD_KEY = "Ord";

    public CompoundTag writeNBT() {
        var tag = new CompoundTag();
        tag.put(POS_KEY, NbtUtils.writeBlockPos(this.pos));
        tag.putString(TEAM_ID_KEY, this.teamID);
        tag.putString(POINT_NAME_KEY, this.pointName);
        tag.putString(PATH_ID_KEY, this.pathID);
        if (this.ord != null) tag.putInt(ORD_KEY, this.ord);
        return tag;
    }

    public static Optional<PathPointData> readNBT(CompoundTag tag) {
        if (tag.contains(POS_KEY, CompoundTag.TAG_COMPOUND)
                && tag.contains(TEAM_ID_KEY, CompoundTag.TAG_STRING) && tag.contains(POINT_NAME_KEY, CompoundTag.TAG_STRING)
                && tag.contains(PATH_ID_KEY, CompoundTag.TAG_STRING)) {
            return Optional.of(new PathPointData(NbtUtils.readBlockPos(tag.getCompound(POS_KEY)),
                    tag.getString(TEAM_ID_KEY), tag.getString(POINT_NAME_KEY), tag.getString(PATH_ID_KEY),
                    tag.contains(ORD_KEY, CompoundTag.TAG_INT) ? tag.getInt(ORD_KEY) : null));
        } else {
            return Optional.empty();
        }
    }

    public Component toTextComponent(Level level) {
        return Component.translatable("commands.check_in.path_point_hover",
                Component.translatable("container.check_in.team_id"), this.teamID,
                Component.translatable("container.check_in.point_name"), this.pointName,
                Component.translatable("container.check_in.path_id"), this.pathID,
                Component.translatable("container.check_in.ord"), this.ord,
                pos.getX(), pos.getY(), pos.getZ(), level.dimensionTypeId().location());
    }

    public TeamPathID teamPathID() {
        return new TeamPathID(teamID, pathID);
    }

    public record TeamPathID(String teamID, String pathID) {
        public CompoundTag writeNBT() {
            var tag = new CompoundTag();
            tag.putString(TEAM_ID_KEY, this.teamID);
            tag.putString(PATH_ID_KEY, this.pathID);
            return tag;
        }

        public static Optional<TeamPathID> readNBT(CompoundTag tag) {
            return tag.contains(TEAM_ID_KEY, CompoundTag.TAG_STRING) && tag.contains(PATH_ID_KEY, CompoundTag.TAG_STRING)
                    ? Optional.of(new TeamPathID(tag.getString(TEAM_ID_KEY), tag.getString(PATH_ID_KEY)))
                    : Optional.empty();
        }
    }
}
