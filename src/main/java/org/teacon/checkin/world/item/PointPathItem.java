package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.world.level.block.PointPathBlock;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

public class PointPathItem extends BlockItem {
    public PointPathItem(Block block, Properties properties) { super(block, properties); }

    /**
     * @see net.minecraft.world.item.SignItem#updateCustomBlockEntityTag(BlockPos, Level, Player, ItemStack, BlockState)
     */
    @Override
    @SuppressWarnings("JavadocReference")
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack itemStack, BlockState state) {
        boolean flag = super.updateCustomBlockEntityTag(pos, level, player, itemStack, state);
        if (!level.isClientSide && !flag && player != null) {
            if (level.getBlockEntity(pos) instanceof PointPathBlockEntity pointPathBlockEntity
                    && level.getBlockState(pos).getBlock() instanceof PointPathBlock) {
                NetworkHooks.openScreen((ServerPlayer) player, pointPathBlockEntity, pos);
            }
        }
        return flag;
    }
}
