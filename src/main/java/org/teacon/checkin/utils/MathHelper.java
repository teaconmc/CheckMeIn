package org.teacon.checkin.utils;

import net.minecraft.core.BlockPos;

public class MathHelper {
    public static int chebyshevDist(BlockPos p1, BlockPos p2) {
        return Math.max(Math.max(
                        Math.abs(p1.getX() - p2.getX()),
                        Math.abs(p1.getY() - p2.getY())),
                Math.abs(p1.getZ() - p2.getZ()));
    }
}
