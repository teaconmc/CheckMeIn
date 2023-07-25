package org.teacon.checkin.world.level.block.entity;

import net.minecraft.core.BlockPos;
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
    public PointUniqueBlockEntity(BlockPos pos, BlockState blockState) {
        super(CheckMeIn.POINT_UNIQUE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getName() {return Component.translatable("container.check_in.point_unique");}

    @Override
    public Component getDisplayName() {return this.getName();}

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.removeIfInvalid()) {
            CheckMeIn.LOGGER.info("Remove invalid %s at %d, %d, %d (%s)", CheckMeIn.POINT_UNIQUE_BLOCk.get(),
                    this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ(), this.level.dimensionTypeId().registry());
        }
    }

    /**
     * Remove when data is not initialized, i.e. data is not in the capability.
     * <p>
     * TODO: is there any uncovered cases in which may an invalid block may exist?
     *
     * @return true if the block is not initialized, otherwise false
     */
    public boolean removeIfInvalid() {
        if (this.level.getCapability(CheckInPoints.Provider.CAPABILITY).map(cap -> cap.getUniquePoint(this.getBlockPos()) == null)
                .orElse(true)) {
            this.level.removeBlock(this.getBlockPos(), false);
            return true;
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory inventory, Player player) {
        return new PointUniqueMenu(p_39954_, this.getBlockPos());
    }
}
