package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PointUniqueItem extends BlockItem {
    public PointUniqueItem(Block block, Properties properties) { super(block, properties); }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return super.placeBlock(context, state);
    }

    /**
     * @see net.minecraft.world.item.SignItem#updateCustomBlockEntityTag(BlockPos, Level, Player, ItemStack, BlockState)
     */
    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, @Nullable Player player, ItemStack itemStack, BlockState state) {
        boolean flag = super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, state);
        if (!level.isClientSide && !flag && player != null) {
            // TODO: call NetworkHooks.openScreen(); after several checks
        }
        return flag;
    }
}
