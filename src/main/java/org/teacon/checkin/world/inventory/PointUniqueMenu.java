package org.teacon.checkin.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.UniquePointData;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

public class PointUniqueMenu extends AbstractContainerMenu {

    private final UniquePointData data;

    public PointUniqueMenu(int windowId, @SuppressWarnings("unused") Inventory inventory, FriendlyByteBuf buf) {
        this(windowId, UniquePointData.readFromBuf(buf));
    }

    public PointUniqueMenu(int windowId, UniquePointData data) {
        super(CheckMeIn.POINT_UNIQUE_MENU.get(), windowId);
        this.data = data;
    }

    public UniquePointData getData() {return data;}

    @Override
    public ItemStack quickMoveStack(Player player, int p_38942_) {throw new UnsupportedOperationException();}

    @Override
    public void removed(Player player) {
        super.removed(player);
        var level = player.level();
        if (!level.isClientSide && level.getBlockEntity(this.data.pos()) instanceof PointUniqueBlockEntity blockEntity) {
            blockEntity.removeIfInvalid();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        var blockEntity = player.level().getBlockEntity(this.data.pos());
        return blockEntity != null && Container.stillValidBlockEntity(blockEntity, player);
    }
}
