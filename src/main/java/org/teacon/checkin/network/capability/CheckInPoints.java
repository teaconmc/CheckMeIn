package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.CheckMeIn;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@AutoRegisterCapability
public class CheckInPoints {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "points");
    private final Map<String, BlockPos> uniquePointMap;

    public CheckInPoints() {this.uniquePointMap = new HashMap<>();}

    public boolean uniquePointExists(String teamID) {return this.uniquePointMap.containsKey(teamID);}

    @javax.annotation.Nullable
    public BlockPos getUniquePoint(String teamID) {return this.uniquePointMap.get(teamID);}

    public boolean addUniquePoint(String teamID, BlockPos blockPos) {
        return uniquePointMap.putIfAbsent(teamID, blockPos) == null;
    }

    public boolean removeUniquePoint(String teamID) {return uniquePointMap.remove(teamID) != null;}

    public Stream<BlockPos> getAllUniquePoints() {return this.uniquePointMap.values().stream();}

    public void write(CompoundTag tag) {
        if (!this.uniquePointMap.isEmpty()) {
            var map = new CompoundTag();
            uniquePointMap.forEach((k, v) -> map.putIntArray(k, new int[]{v.getX(), v.getY(), v.getZ()}));
            tag.put("UniquePoints", map);
        }
    }

    public void read(CompoundTag tag) {
        if (tag.get("UniquePoints") instanceof CompoundTag compoundTag) {
            this.uniquePointMap.clear();
            for (var k : compoundTag.getAllKeys()) {
                var t = compoundTag.get(k);
                if (!(t instanceof IntArrayTag)) continue;
                var arr = ((IntArrayTag) t).getAsIntArray();
                if (arr.length != 3) continue;
                this.uniquePointMap.put(k, new BlockPos(arr[0], arr[1], arr[2]));
            }
        }

    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        public static final Capability<CheckInPoints> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

        private final CheckInPoints checkInPoints = new CheckInPoints();
        private final LazyOptional<CheckInPoints> capability = LazyOptional.of(() -> checkInPoints);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
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
