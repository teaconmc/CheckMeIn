package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.world.level.block.PointUniqueBlock;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

public class PointUniqueItem extends BlockItem {
    public PointUniqueItem(Block block, Properties properties) { super(block, properties); }

    /**
     * Open menu and screen after placing block for editing
     */
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack itemStack, BlockState state) {
        boolean flag = super.updateCustomBlockEntityTag(pos, level, player, itemStack, state);
        if (!level.isClientSide && !flag && player != null) {
            if (level.getBlockEntity(pos) instanceof PointUniqueBlockEntity pointUniqueBlockEntity
                    && level.getBlockState(pos).getBlock() instanceof PointUniqueBlock) {
                pointUniqueBlockEntity.openScreen((ServerPlayer) player);
            }
        }
        return flag;
    }
}
