package org.teacon.checkin.network.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.utils.NbtHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@AutoRegisterCapability
public class CheckProgress {

    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "progress");
    private static final String UNIQUE_POINTS_KEY = "UniquePoints";
    private static final String PATH_PROGRESS_KEY = "PathProgress";
    private static final String PROGRESS_KEY = "Progress";

    private final Map<String, UniquePointData> uniquePoints = new HashMap<>();
    private final Map<PathPointData.TeamPathID, Integer> pathProgress = new HashMap<>();

    public boolean checked(UniquePointData data) {return uniquePoints.containsKey(data.teamID());}

    /**
     * Check a unique point only when it is not previously checked
     *
     * @return if a new point is checked
     */
    public boolean checkUniquePoint(UniquePointData data) {return uniquePoints.putIfAbsent(data.teamID(), data) == null;}

    /**
     * Advance the check-in progress
     */
    public void checkPathPoint(PathPointData.TeamPathID id, int ord) {pathProgress.compute(id, (k, v) -> v == null || v < ord ? ord : v);}

    public void resetUniquePoint(String teamID) {uniquePoints.remove(teamID);}

    public void resetAllUniquePoints() {uniquePoints.clear();}

    public void resetPath(String teamID, String pathID) {pathProgress.remove(new PathPointData.TeamPathID(teamID, pathID));}

    public Collection<String> checkedUniquePointTeamIDs() {return uniquePoints.keySet();}

    /*                  Persistence                 */
    public void write(CompoundTag tag) {
        tag.put(UNIQUE_POINTS_KEY, this.uniquePoints.values().stream().map(UniquePointData::writeNBT).collect(NbtHelper.toListTag()));
        tag.put(PATH_PROGRESS_KEY, this.pathProgress.entrySet().stream().map(entry -> {
            var compound = entry.getKey().writeNBT();
            compound.putInt(PROGRESS_KEY, entry.getValue());
            return compound;
        }).collect(NbtHelper.toListTag()));
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
                    if (ct.contains(PROGRESS_KEY, CompoundTag.TAG_INT))
                        this.checkPathPoint(id, ct.getInt(PROGRESS_KEY));
                });
            }
        }
    }

    public static LazyOptional<CheckProgress> of(ServerPlayer player) {
        return player.getCapability(Provider.CAPABILITY);
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
