package org.teacon.checkin.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.level.Level;

public class TextComponent {
    public static ClickEvent teleportTo(BlockPos blockPos, Level level) {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/execute as @p in %s run teleport %d %d %d"
                .formatted(level.dimensionTypeId().location(), blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }
}
