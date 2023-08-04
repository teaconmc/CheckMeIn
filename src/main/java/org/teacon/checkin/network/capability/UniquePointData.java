package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record UniquePointData(BlockPos pos, String teamID, String pointName) {
    private static final String POS_KEY = "Pos";
    private static final String TEAM_ID_KEY = "TeamID";
    private static final String POINT_NAME_KEY = "PointName";

    public CompoundTag writeNBT() {
        var tag = new CompoundTag();
        tag.put(POS_KEY, NbtUtils.writeBlockPos(this.pos));
        tag.putString(TEAM_ID_KEY, this.teamID);
        tag.putString(POINT_NAME_KEY, this.pointName);
        return tag;
    }

    public static Optional<UniquePointData> readNBT(CompoundTag tag) {
        if (tag.contains(POS_KEY, CompoundTag.TAG_COMPOUND)
                && tag.contains(TEAM_ID_KEY, CompoundTag.TAG_STRING) && tag.contains(POINT_NAME_KEY, CompoundTag.TAG_STRING)) {
            return Optional.of(new UniquePointData(NbtUtils.readBlockPos(tag.getCompound(POS_KEY)),
                    tag.getString(TEAM_ID_KEY), tag.getString(POINT_NAME_KEY)));
        } else {
            return Optional.empty();
        }
    }

    public Component toTextComponent(Level level) {
        return Component.translatable("commands.check_in.unique_point_hover",
                Component.translatable("container.check_in.team_id"), this.teamID,
                Component.translatable("container.check_in.point_name"), this.pointName,
                pos.getX(), pos.getY(), pos.getZ(), level.dimensionTypeId().location());
    }
}
