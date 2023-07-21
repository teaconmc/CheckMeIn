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

    public void setTeamID(String teamID) {this.teamID = teamID;}

    public void setPointName(String pointName) {this.pointName = pointName;}

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

    // TODO: is there any uncovered cases which may leave an invalid block?
    public void removeIfInvalid() {
        // TODO
    }

    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory inventory, Player player) {
        return new PointUniqueMenu(p_39954_, this.getBlockPos());
    }
}
