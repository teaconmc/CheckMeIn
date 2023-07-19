package org.teacon.checkin.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.teacon.checkin.CheckMeIn;

public class PointUniqueBlockEntity extends BlockEntity implements Nameable {
    public PointUniqueBlockEntity(BlockPos pos, BlockState blockState) {
        super(CheckMeIn.POINT_UNIQUE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getName() {
        return null;
    }
}
