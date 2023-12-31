package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is for syncing non-persistent data from server to client.
 * <p>
 * The data should only be used for rendering purpose, and should not be involved in business logic
 */
@AutoRegisterCapability
public class GuidingManager {
    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "guiding_render");
    public final ClientFace clientFace = new ClientFace();
    public final ServerFace serverFace = new ServerFace();

    public static LazyOptional<GuidingManager> of(Player player) {return player.getCapability(Provider.CAPABILITY);}


    public static class ClientFace {
        private final List<BlockPos> pathPlannerPoints = new ArrayList<>();

        @Nullable
        private GlobalPos pathNavNextPoint = null;

        @Nullable
        private GlobalPos nearbyUniquePoint = null;

        public List<BlockPos> getPathPlannerPoints() {return pathPlannerPoints;}

        public void setPathPlannerPoints(List<BlockPos> pathPlannerPoints) {
            this.pathPlannerPoints.clear();
            this.pathPlannerPoints.addAll(pathPlannerPoints);
        }

        @Nullable
        public GlobalPos getPathNavNextPoint() {return pathNavNextPoint;}

        public void setPathNavNextPoint(@Nullable GlobalPos pathNavNextPoint) {this.pathNavNextPoint = pathNavNextPoint;}

        @Nullable
        public GlobalPos getNearbyUniquePoint() {return nearbyUniquePoint;}

        public void setNearbyUniquePoint(@Nullable GlobalPos nearbyUniquePoint) {this.nearbyUniquePoint = nearbyUniquePoint;}
    }

    public static class ServerFace {
        @Nullable
        private PathPointData.TeamPathID pathPlannerFocusID = null;
        @Nullable
        private ResourceKey<Level> dimension = null;

        @Nullable
        public PathPointData.TeamPathID getPathPlannerFocusID() {return pathPlannerFocusID;}

        public void setPathPlannerFocusID(@Nullable PathPointData.TeamPathID pathPlannerFocusID) {
            this.pathPlannerFocusID = pathPlannerFocusID;
        }

        @Nullable
        public ResourceKey<Level> getDimension() {return dimension;}

        public void setDimension(@Nullable ResourceKey<Level> dimension) {this.dimension = dimension;}
    }

    public static class Provider implements ICapabilityProvider {
        public static final Capability<GuidingManager> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

        private final GuidingManager guidingManager = new GuidingManager();
        private final LazyOptional<GuidingManager> capability = LazyOptional.of(() -> guidingManager);

        @Override
        public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.capability);
        }
    }
}
