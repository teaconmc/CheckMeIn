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
import org.teacon.checkin.world.inventory.PointUniqueMenu;

public class PointUniqueBlockEntity extends BlockEntity implements Nameable, MenuProvider {

    private String teamID = "";
    private String pointName = "";

    public PointUniqueBlockEntity(BlockPos pos, BlockState blockState) {
        super(CheckMeIn.POINT_UNIQUE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getName() {return Component.translatable("container.check_in.point_unique");}

    @Override
    public Component getDisplayName() {return this.getName();}

    public void setTeamID(String teamID) {
        this.teamID = teamID;
        this.level.getCapability(CheckInPoints.Provider.CAPABILITY)
                .ifPresent(cap -> cap.addUniquePoint(this.teamID, this.getBlockPos()));
    }

    public void setPointName(String pointName) {this.pointName = pointName;}

    public String getTeamID() {return teamID;}

    public String getPointName() {return pointName;}

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.teamID = compoundTag.getString("TeamID");
        this.pointName = compoundTag.getString("PointName");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.putString("TeamID", this.teamID);
        compoundTag.putString("PointName", this.pointName);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.removeIfInvalid();
    }

    // TODO: is there any uncovered cases in which may an invalid block may exist?
    public void removeIfInvalid() {
        if (this.teamID.isEmpty() || !this.level.getCapability(CheckInPoints.Provider.CAPABILITY)
                .map(cap -> cap.uniquePointExists(this.teamID)).orElse(true)) {
            this.level.removeBlock(this.getBlockPos(), false);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory inventory, Player player) {
        return new PointUniqueMenu(p_39954_, this.getBlockPos());
    }
}
