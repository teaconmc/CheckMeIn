package org.teacon.checkin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.PathPointData;
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
            if (player.getMainHandItem().is(CheckMeIn.PATH_PLANNER.get())) {

                var compoundTag = player.getMainHandItem().getOrCreateTagElement(PathPlanner.PLANNER_PROPERTY_KEY);
                if (PathPlanner.isTagIncomplete(compoundTag)) {
                    // no path is selected (fresh planner), or item data is missing
                    ((ServerPlayer) player).sendSystemMessage(Component.translatable("item.check_in.path_planner.unselected"), true);
                    if (level.getBlockEntity(pos) instanceof PointPathBlockEntity pointPathBlockEntity)
                        pointPathBlockEntity.removeIfInvalid();
                } else {
                    CheckInPoints.of(level).ifPresent(cap -> PathPlanner.updateAndIncOrd(compoundTag, cap,
                            new PathPointData(pos, compoundTag.getString(PathPlanner.TEAM_ID_KEY), compoundTag.getString(PathPlanner.POINT_NAME_KEY), compoundTag.getString(PathPlanner.PATH_ID_KEY), null),
                            (ServerLevel) level, (ServerPlayer) player));
                }
            } else if (level.getBlockEntity(pos) instanceof PointPathBlockEntity pointPathBlockEntity
                    && level.getBlockState(pos).getBlock() instanceof PointPathBlock) {
                NetworkHooks.openScreen((ServerPlayer) player, pointPathBlockEntity, pos);
            }
        }
        return flag;
    }
}
