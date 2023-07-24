package org.teacon.checkin.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.world.inventory.PointUniqueMenu;

import javax.annotation.Nullable;

public class PointUniqueBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    private static final String POINT_DATA_KEY = "PointData";

    @Nullable
    private UniquePointData pointData = null;

    public PointUniqueBlockEntity(BlockPos pos, BlockState blockState) {
        super(CheckMeIn.POINT_UNIQUE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getName() {return Component.translatable("container.check_in.point_unique");}

    @Override
    public Component getDisplayName() {return this.getName();}

    public void initializeData(String teamID, String pointName) {
        if (this.pointData != null) return;
        this.pointData = new UniquePointData(this.getBlockPos(), teamID, pointName);
        this.level.getCapability(CheckInPoints.Provider.CAPABILITY)
                .ifPresent(cap -> cap.addUniquePoint(teamID, this.pointData));
    }

    @Nullable
    public UniquePointData getPointData() {return this.pointData;}

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.pointData = UniquePointData.readNBT(compoundTag.getCompound(POINT_DATA_KEY)).orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        if (this.pointData != null) compoundTag.put(POINT_DATA_KEY, this.pointData.writeNBT());
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.removeIfInvalid();
    }

    /**
     * Remove when data is not initialized, or the point's data does match that in the capability.
     * TODO: is there any uncovered cases in which may an invalid block may exist?
     */
    public void removeIfInvalid() {
        if (this.pointData == null || !this.level.getCapability(CheckInPoints.Provider.CAPABILITY)
                .map(cap -> this.pointData.equals(cap.getUniquePoint(this.pointData.teamID())))
                .orElse(true)) {
            this.level.removeBlock(this.getBlockPos(), false);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory inventory, Player player) {
        return new PointUniqueMenu(p_39954_, this.getBlockPos());
    }
}
