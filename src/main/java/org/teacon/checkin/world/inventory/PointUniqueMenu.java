package org.teacon.checkin.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

public class PointUniqueMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public PointUniqueMenu(int windowId, Inventory inventory, FriendlyByteBuf buf) {
        this(windowId, buf.readBlockPos());
    }

    public PointUniqueMenu(int windowId, BlockPos blockPos) {
        super(CheckMeIn.POINT_UNIQUE_MENU.get(), windowId);
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int p_38942_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        var level = player.level();
        if (!level.isClientSide &&
                level.getBlockEntity(this.blockPos) instanceof PointUniqueBlockEntity pointUniqueBlockEntity) {
            pointUniqueBlockEntity.removeIfInvalid();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        var blockEntity = player.level().getBlockEntity(this.blockPos);
        return blockEntity != null && Container.stillValidBlockEntity(blockEntity, player);
    }
}
