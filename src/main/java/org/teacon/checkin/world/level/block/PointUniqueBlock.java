package org.teacon.checkin.world.level.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.configs.ClientConfig;
import org.teacon.checkin.configs.CommonConfig;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.network.capability.GuidingManager;
import org.teacon.checkin.utils.MathHelper;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

import java.util.Collection;
import java.util.List;

import static org.teacon.checkin.world.level.block.PointPathBlock.drawParticles;

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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        PointUniqueBlock.PointPathBlockFX.handle0(state, level, pos);
    }


    private static final class  PointPathBlockFX {

        private static final Vector3f GOLD_COLOR = new Vector3f(1, 1, 0);
        private static final Vector3f GREEN_COLOR = new Vector3f(0, 1, 0);
        private static final Vector3f RED_COLOR = new Vector3f(1, 0, 0);

        static void handle0(BlockState state, Level level, BlockPos pos) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (!player.isHolding(CheckMeIn.CHECKER.get())) return;
            if (!Vec3.atCenterOf(pos).closerThan(player.position(), ClientConfig.INSTANCE.checkPointSuggestDistance.get())) return;

            pos = pos.immutable();
            var globalPos = GlobalPos.of(level.dimension(), pos);
            var prevNearby = GuidingManager.of(player).resolve().map(manager -> manager.clientFace.getNearbyUniquePoint());
            final float r = CommonConfig.INSTANCE.uniquePointCheckInRange.get();
            var isNearby = MathHelper.chebyshevDist(pos, player.blockPosition()) <= r;
            if (prevNearby.isEmpty()) {
                if (isNearby) {
                    // previously not nearby any point but near current point now
                    drawParticles(level, pos.getCenter(),  r + 0.5, GREEN_COLOR, 0.25f);
                    GuidingManager.of(player).ifPresent(manager -> manager.clientFace.setNearbyUniquePoint(globalPos));
                } else {
                    // always not nearby any point but within suggest range
                    drawParticles(level, pos.getCenter(), r + 0.5, GOLD_COLOR, 0.25f);
                }
            } else {
                if (prevNearby.get().equals(globalPos) && !isNearby) {
                    // previously nearby but now leaves current point
                    drawParticles(level, pos.getCenter(), r + 0.5, RED_COLOR, 0.25f);
                    GuidingManager.of(player).ifPresent(manager -> manager.clientFace.setNearbyUniquePoint(null));
                }
            }
        }
    }
}
