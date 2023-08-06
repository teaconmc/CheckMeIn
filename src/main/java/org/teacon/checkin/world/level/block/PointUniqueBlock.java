package org.teacon.checkin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.Collection;
import java.util.List;

public class PointUniqueBlock extends AbstractCheckPointBlock {
    public static final String NAME = "point_unique";

    public PointUniqueBlock(Properties prop) {super(prop);}

    @Override
    public Collection<Item> getRevealingItems() {return List.of(CheckMeIn.POINT_UNIQUE_ITEM.get());}

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {return new PointUniqueBlockEntity(pos, state);}

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState state, boolean p_60519_) {
        if (!oldState.is(state.getBlock())) {
            CheckInPoints.of(level).ifPresent(cap -> cap.removeUniquePoint(pos));
            super.onRemove(oldState, level, pos, state, p_60519_);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PointUniqueBlockEntity blockEntity && player.canUseGameMasterBlocks()) {
            if (!level.isClientSide) blockEntity.openScreen((ServerPlayer) player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
