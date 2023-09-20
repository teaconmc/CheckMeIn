package org.teacon.checkin.world.level.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import org.teacon.checkin.world.level.block.entity.PointPathBlockEntity;

import java.util.Collection;
import java.util.List;

public class PointPathBlock extends AbstractCheckPointBlock {
    public static final String NAME = "point_path";

    public PointPathBlock(Properties prop) {super(prop);}

    @Override
    public Collection<Item> getRevealingItems() {return List.of(CheckMeIn.POINT_PATH_ITEM.get(), CheckMeIn.PATH_PLANNER.get());}

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

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.getItemInHand(hand).is(CheckMeIn.PATH_PLANNER.get())
                && level.getBlockEntity(pos) instanceof PointPathBlockEntity blockEntity
                && player.canUseGameMasterBlocks()) {
            if (!level.isClientSide) blockEntity.openScreen((ServerPlayer) player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        PointPathBlockFX.handle0(state, level, pos);
    }

    private static final class PointPathBlockFX {

        private static final Vector3f CYAN_COLOR = new Vector3f(0, 1, 1);
        static void handle0(BlockState state, Level level, BlockPos pos) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (!ClientConfig.INSTANCE.pathNaviAlwaysSuggest.get() && !player.isHolding(CheckMeIn.NVG_PATH.get())) return;
            pos = pos.immutable();
            var globalPos = GlobalPos.of(level.dimension(), pos);
            if (!GuidingManager.of(player).resolve().map(manager -> globalPos.equals(manager.clientFace.getPathNavNextPoint())).orElse(false))
                return;
            if (!Vec3.atCenterOf(pos).closerThan(player.position(), ClientConfig.INSTANCE.checkPointSuggestDistance.get()))
                return;
            drawParticles(level, pos.getCenter(), CommonConfig.INSTANCE.pathPointCheckInRange.get() + 0.5, CYAN_COLOR, 0.25f);
        }
    }

    public static void drawParticles(Level level, Vec3 pos, double r, Vector3f color, float d) {
        for (var dx = -r; dx <= r; dx += d) {
            level.addParticle(new DustParticleOptions(color, 1F), pos.x + dx, pos.y, pos.z + r, 0.0, 0.0, 0.0);
            level.addParticle(new DustParticleOptions(color, 1F), pos.x + dx, pos.y, pos.z - r, 0.0, 0.0, 0.0);
        }
        for (var dz = -r; dz <= r; dz += d) {
            level.addParticle(new DustParticleOptions(color, 1F), pos.x + r, pos.y, pos.z + dz, 0.0, 0.0, 0.0);
            level.addParticle(new DustParticleOptions(color, 1F), pos.x - r, pos.y, pos.z - dz, 0.0, 0.0, 0.0);
        }
    }
}
