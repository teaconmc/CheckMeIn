package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.world.level.block.PointUniqueBlock;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

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
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack itemStack, BlockState state) {
        boolean flag = super.updateCustomBlockEntityTag(pos, level, player, itemStack, state);
        if (!level.isClientSide && !flag && player != null) {
            if (level.getBlockEntity(pos) instanceof PointUniqueBlockEntity pointUniqueBlockEntity
                    && level.getBlockState(pos).getBlock() instanceof PointUniqueBlock) {
                NetworkHooks.openScreen((ServerPlayer) player, pointUniqueBlockEntity, pos);
            }
        }
        return flag;
    }
}
