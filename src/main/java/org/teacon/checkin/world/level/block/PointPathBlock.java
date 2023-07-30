package org.teacon.checkin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

import java.util.Collection;
import java.util.List;

public class PointPathBlock extends AbstractCheckPointBlock {
    public static final String NAME = "point_path";

    public PointPathBlock(Properties prop) {super(prop);}

    @Override
    protected Collection<Item> getRevealingItems() {return List.of(CheckMeIn.POINT_PATH_ITEM.get(), CheckMeIn.PATH_PLANNER.get());}

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {return new PointPathBlockEntity(pos, state);}

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState state, boolean p_60519_) {
        if (!oldState.is(state.getBlock())) {
            CheckInPoints.of(level).ifPresent(cap -> cap.removePathPoint(pos));
            super.onRemove(oldState, level, pos, state, p_60519_);
        }
    }
}
