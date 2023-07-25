package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@AutoRegisterCapability
public class CheckInPoints {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "points");
    private final Map<BlockPos, UniquePointData> blockPosUniquePointMap = new HashMap<>();
    private final Map<String, UniquePointData> teamIDUniquePointMap = new HashMap<>();

    public CheckInPoints() {}

    @Nullable
    public UniquePointData getUniquePoint(BlockPos pos) {return this.blockPosUniquePointMap.get(pos);}

    @Nullable
    public UniquePointData getUniquePoint(String teamID) {return this.teamIDUniquePointMap.get(teamID);}

    public boolean addUniquePoint(UniquePointData pointData) {
        if (!blockPosUniquePointMap.containsKey(pointData.pos()) && !teamIDUniquePointMap.containsKey(pointData.teamID())) {
            blockPosUniquePointMap.put(pointData.pos(), pointData);
            teamIDUniquePointMap.put(pointData.teamID(), pointData);
            return true;
        }
        return false;
    }

    public boolean removeUniquePoint(BlockPos pos) {
        var data = blockPosUniquePointMap.remove(pos);
        if (data == null) return false;
        teamIDUniquePointMap.remove(data.teamID());
        return true;
    }

    public Collection<UniquePointData> getAllUniquePoints() {return this.blockPosUniquePointMap.values();}


    public void write(CompoundTag tag) {
        if (!this.teamIDUniquePointMap.isEmpty()) {
            var list = new ListTag();
            getAllUniquePoints().stream().map(UniquePointData::writeNBT).forEach(list::add);
            tag.put("UniquePoints", list);
        }
    }

    public void read(CompoundTag tag) {
        if (tag.contains("UniquePoints", CompoundTag.TAG_LIST)) {
            this.blockPosUniquePointMap.clear();
            this.teamIDUniquePointMap.clear();
            for (var t : tag.getList("UniquePoints", CompoundTag.TAG_COMPOUND)) {
                if (t instanceof CompoundTag ct) UniquePointData.readNBT(ct).ifPresent(data -> {
                    this.blockPosUniquePointMap.put(data.pos(), data);
                    this.teamIDUniquePointMap.put(data.teamID(), data);
                });
            }
        }
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
