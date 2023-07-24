package org.teacon.checkin.network.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@AutoRegisterCapability
public class CheckInPoints {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "points");
    private final Map<String, UniquePointData> teamIDUniquePointMap;

    public CheckInPoints() {this.teamIDUniquePointMap = new HashMap<>();}

    public boolean uniquePointExists(String teamID) {return this.teamIDUniquePointMap.containsKey(teamID);}

    @Nullable
    public UniquePointData getUniquePoint(String teamID) {return this.teamIDUniquePointMap.get(teamID);}

    public boolean addUniquePoint(String teamID, UniquePointData pointData) {
        return teamIDUniquePointMap.putIfAbsent(teamID, pointData) == null;
    }

    public boolean removeUniquePoint(String teamID) {return teamIDUniquePointMap.remove(teamID) != null;}

    public Iterable<UniquePointData> getAllUniquePoints() {return this.teamIDUniquePointMap.values();}

    public void write(CompoundTag tag) {
        if (!this.teamIDUniquePointMap.isEmpty()) {
            var map = new CompoundTag();
            teamIDUniquePointMap.forEach((k, v) -> map.put(k, v.writeNBT()));
            tag.put("UniquePoints", map);
        }
    }

    public void read(CompoundTag tag) {
        if (tag.get("UniquePoints") instanceof CompoundTag compoundTag) {
            this.teamIDUniquePointMap.clear();
            for (var k : compoundTag.getAllKeys()) {
                UniquePointData.readNBT(compoundTag.getCompound(k))
                        .ifPresent(v -> this.teamIDUniquePointMap.put(k, v));
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
