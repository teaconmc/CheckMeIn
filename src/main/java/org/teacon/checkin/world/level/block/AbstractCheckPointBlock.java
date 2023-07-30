package org.teacon.checkin.world.level.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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

import java.util.Collection;

public abstract class AbstractCheckPointBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Minecraft mc;

    public AbstractCheckPointBlock(BlockBehaviour.Properties prop) {
        super(prop);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, Boolean.FALSE));
        this.mc = Minecraft.getInstance();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(WATERLOGGED);}

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        return getRevealingItems().stream().anyMatch(collisionContext::isHoldingItem) ? Shapes.block() : Shapes.empty();
    }

    protected abstract Collection<Item> getRevealingItems();

    @Override
    public boolean propagatesSkylightDown(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {return true;}

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public RenderShape getRenderShape(BlockState blockState) {return RenderShape.INVISIBLE;}

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {return 1.0F;}

    @Override
    @SuppressWarnings("deprecation")
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

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos pos, RandomSource randomSource) {
        if (this.mc.gameMode != null && this.mc.gameMode.getPlayerMode() == GameType.CREATIVE) {
            if (this.mc.player != null && this.getRevealingItems().contains(this.mc.player.getMainHandItem().getItem())) {
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        0, 0, 0);
            }
        }
    }
}
