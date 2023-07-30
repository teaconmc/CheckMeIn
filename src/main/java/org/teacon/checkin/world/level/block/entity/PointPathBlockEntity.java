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
import org.teacon.checkin.world.inventory.PointPathMenu;

import java.util.Objects;

public class PointPathBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    public PointPathBlockEntity(BlockPos pos, BlockState blockState) {
        super(CheckMeIn.POINT_PATH_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getName() {return Component.translatable("container.check_in.point_unique");}

    @Override
    public Component getDisplayName() {return this.getName();}

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.removeIfInvalid()) {
            CheckMeIn.LOGGER.info("Remove invalid {} at {}, {}, {} ({})", CheckMeIn.POINT_PATH_BLOCK.get(),
                    this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ(), this.level != null ? this.level.dimensionTypeId().registry() : "null");
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
        if (this.level != null && CheckInPoints.of(this.level).map(cap -> cap.getPathPoint(this.getBlockPos()) == null)
                .orElse(true)) {
            this.level.removeBlock(this.getBlockPos(), false);
            return true;
        }
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory inventory, Player player) {
        return new PointPathMenu(p_39954_, this.getBlockPos()); // BlockPos is delivered to menu in FriendlyByteBuf
    }
}
