package org.teacon.checkin.world.level.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.teacon.checkin.CheckMeIn;
import org.teacon.checkin.network.capability.CheckInPoints;
import org.teacon.checkin.world.level.block.entity.PointUniqueBlockEntity;

public class PointUniqueBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final String NAME = "point_unique";

    public PointUniqueBlock(BlockBehaviour.Properties prop) {
        super(prop);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(WATERLOGGED);}

    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        return collisionContext.isHoldingItem(CheckMeIn.POINT_UNIQUE_ITEM.get()) ? Shapes.block() : Shapes.empty();
    }

    public boolean propagatesSkylightDown(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {return true;}

    public RenderShape getRenderShape(BlockState blockState) {return RenderShape.INVISIBLE;}

    public float getShadeBrightness(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {return 1.0F;}

    public BlockState updateShape(BlockState blockState,
                                  Direction direction,
                                  BlockState blockState1,
                                  LevelAccessor levelAccessor,
                                  BlockPos blockPos,
                                  BlockPos blockPos1) {
        if (blockState.getValue(WATERLOGGED))
            levelAccessor.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
        return super.updateShape(blockState, direction, blockState1, levelAccessor, blockPos, blockPos1);
    }

    public FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos pos, RandomSource randomSource) {
        if (Minecraft.getInstance().gameMode.getPlayerMode() == GameType.CREATIVE) {
            ItemStack itemstack = Minecraft.getInstance().player.getMainHandItem();
            Item item = itemstack.getItem();
            if (item instanceof BlockItem blockItem && blockItem.getBlock() == CheckMeIn.POINT_UNIQUE_BLOCk.get()) {
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        0, 0, 0);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {return new PointUniqueBlockEntity(pos, state);}

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState state, boolean p_60519_) {
        if (!oldState.is(state.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PointUniqueBlockEntity pointUniqueBE) {
                level.getCapability(CheckInPoints.Provider.CAPABILITY)
                        .ifPresent(cap -> cap.removeUniquePoint(pointUniqueBE.getTeamID()));
            }
            super.onRemove(oldState, level, pos, state, p_60519_);
        }
    }
}
