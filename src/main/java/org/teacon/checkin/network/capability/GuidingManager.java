package org.teacon.checkin.network.capability;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.teacon.checkin.CheckMeIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@AutoRegisterCapability
public class GuidingManager {
    public static final ResourceLocation ID = new ResourceLocation(CheckMeIn.MODID, "guiding_render");
    private ImmutableList<BlockPos> pathPlannerPoints = ImmutableList.of();
    @Nullable
    private BlockPos pathPlannerFocus = null;

    public static LazyOptional<GuidingManager> of(Player player) {return player.getCapability(Provider.CAPABILITY);}

    public ImmutableList<BlockPos> getPathPlannerPoints() {return pathPlannerPoints;}

    public void setPathPlannerPoints(List<BlockPos> pathPlannerPoints) {
        this.pathPlannerPoints = ImmutableList.copyOf(pathPlannerPoints);
    }

    @Nullable
    public BlockPos getPathPlannerFocus() {return pathPlannerFocus;}

    public void setPathPlannerFocus(@Nullable BlockPos pathPlannerFocus) {this.pathPlannerFocus = pathPlannerFocus;}

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
