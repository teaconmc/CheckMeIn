package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record UniquePointData(BlockPos pos, String teamID, String pointName) {
    private static final String POS_KEY = "Pos";
    private static final String TEAM_ID_KEY = "TeamID";
    private static final String POINT_NAME_KEY = "PointName";

    public static final int TEAM_ID_MAX_LENGTH = 50;
    public static final int POINT_NAME_MAX_LENGTH = 50;

    public static UniquePointData empty(BlockPos pos) {
        return new UniquePointData(pos, "", "");
    }

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

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.teamID);
        buf.writeUtf(this.pointName);
    }

    public static UniquePointData readFromBuf(FriendlyByteBuf buf) {
        return new UniquePointData(buf.readBlockPos(), buf.readUtf(), buf.readUtf());
    }

    public Component toTextComponent(Level level) {
        return Component.translatable("commands.check_in.unique_point_hover",
                Component.translatable("container.check_in.team_id"), this.teamID,
                Component.translatable("container.check_in.point_name"), this.pointName,
                pos.getX(), pos.getY(), pos.getZ(), level.dimension().location());
    }
}
