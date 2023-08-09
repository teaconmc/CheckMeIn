package org.teacon.checkin.network.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@AutoRegisterCapability
public class GuidingManager {
    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "guiding_render");
    public final ClientFace clientFace = new ClientFace();
    public final ServerFace serverFace = new ServerFace();

    public static LazyOptional<GuidingManager> of(Player player) {return player.getCapability(Provider.CAPABILITY);}


    public static class ClientFace {
        private final List<BlockPos> pathPlannerPoints = new ArrayList<>();

        public List<BlockPos> getPathPlannerPoints() {return pathPlannerPoints;}

        public void setPathPlannerPoints(List<BlockPos> pathPlannerPoints) {
            this.pathPlannerPoints.clear();
            this.pathPlannerPoints.addAll(pathPlannerPoints);
        }
    }

    public static class ServerFace {
        @Nullable
        private PathPointData.TeamPathID pathPlannerFocusID = null;

        @Nullable
        public PathPointData.TeamPathID getPathPlannerFocusID() {return pathPlannerFocusID;}

        public void setPathPlannerFocusID(PathPointData.TeamPathID pathPlannerFocusID) {
            this.pathPlannerFocusID = pathPlannerFocusID;
        }
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
